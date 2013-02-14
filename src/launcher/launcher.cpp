//
// Original Work:
//   Copyright (c) 2006, Brian Frank and Andy Frank
//
// Derived Work:
//   Copyright (c) 2007 Tridium, Inc.
//   Licensed under the Academic Free License version 3.0
//
// History:
//   3 Mar 07  Brian Frank  Creation
//

#include <stdio.h>
#include <Windows.h>
#include <jni.h>

#ifndef LAUNCHER_MAIN
#error "Must define LAUNCHER_MAIN"
#endif

//////////////////////////////////////////////////////////////////////////
// TypeDefs
//////////////////////////////////////////////////////////////////////////

typedef jint (JNICALL *CreateJavaVMFunc)(JavaVM **pvm, void **penv, void *vm_args);

//////////////////////////////////////////////////////////////////////////
// Globals
//////////////////////////////////////////////////////////////////////////

const char* LAUNCHER_VERSION = "28-Jan-13";

bool debug;                        // is debug turned on
char sedonaHome[MAX_PATH];         // dir path of sedona installation
int sedonaArgc;                    // argument count to pass to Sedona runtime
char** sedonaArgv;                 // argument values to pass to Sedona runtime
const int MAX_OPTIONS = 32;        // max number of Java options
JavaVMOption options[MAX_OPTIONS]; // Java options to pass to create VM
char jvmPath[MAX_PATH];            // path to jvm.dll to dynamically load
int nOptions;                      // Number of options
JavaVM* vm;                        // VM created
JNIEnv* env;                       // JNI environment

//////////////////////////////////////////////////////////////////////////
// Error Utils
//////////////////////////////////////////////////////////////////////////

/**
 * Print an error message and return -1.
 */
int err(const char* msg, const char* arg1, const char* arg2)
{
  printf("ERROR: ");
  printf(msg, arg1, arg2);
  printf("\n");
  return -1;
}
int err(const char* msg, const char* arg1) { return err(msg, arg1, "ignored"); }
int err(const char* msg) { return err(msg, "ignored", "ignored"); }

//////////////////////////////////////////////////////////////////////////
// Registry Utils
//////////////////////////////////////////////////////////////////////////

/**
 * Read a registry string from HKEY_LOCAL_MACHINE.
 */
int readRegistry(const char* subKey, char* name, char* buf, int bufLen)
{
  // open key (try again as 64-bit key if first attempt fails)
  HKEY hKey;
  if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, subKey, 0, KEY_QUERY_VALUE, &hKey) != ERROR_SUCCESS)
    if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, subKey, 0, KEY_QUERY_VALUE|KEY_WOW64_64KEY, &hKey) != ERROR_SUCCESS)
      return err("[launcher] Cannot open registry key: HKEY_LOCAL_MACHINE\\%s.%s", subKey, name);

  // query
  int query = RegQueryValueEx(hKey, name, NULL, NULL, (LPBYTE)buf, (LPDWORD)&bufLen);

  // close
  RegCloseKey(hKey);

  // return result
  if (query != ERROR_SUCCESS)
    return err("[launcher] Cannot query registry key: HKEY_LOCAL_MACHINE\\%s.%s", subKey, name);

  return 0;
}
//////////////////////////////////////////////////////////////////////////
// Init
//////////////////////////////////////////////////////////////////////////

/**
 * Initialize the global variables for this process's environment.
 */
int init(int argc, char** argv)
{
  // debug controlled by environment variable or --v argument
  debug = getenv("sedona_launcher_debug") != NULL;
  for (int i=1; i<argc; ++i) if (strcmp(argv[i], "--v") == 0) debug = true;
  if (debug)
  {
    printf("-- launcher version %s\n", LAUNCHER_VERSION);
    for (int i=0; i<argc; ++i)
      printf("--   args[%d] = \"%s\"\n", i, argv[i]);
    printf("-- init\n");
  }

  // get my module
  char p[MAX_PATH];
  if (!GetModuleFileName(NULL, p, MAX_PATH))
    return err("[launcher] GetModuleFileName");

  // walk up three levels of the path to get sedona home:
  //   {sedonaHome}\bin\me.exe
  int len = strlen(p);
  for (; len > 0; len--) if (p[len] == '\\') { p[len] = '\0'; break; }
  for (; len > 0; len--) if (p[len] == '\\') { p[len] = '\0'; break; }
  strcpy(sedonaHome, p);
  if (debug) printf("--   sedonaHome = %s\n", sedonaHome);

  return 0;
}

//////////////////////////////////////////////////////////////////////////
// Parse Arguments
//////////////////////////////////////////////////////////////////////////

/**
 * Parse arguments to initialize sedonaArgc and sedonaArgv, plus check for
 * arguments used by the launcher itself (prefixed via --).  Note that
 * the --v debug is handled in init(), not this method.
 */
int parseArgs(int argc, char** argv)
{
  if (debug) printf("-- parseArgs\n");

  sedonaArgc = 0;
  sedonaArgv = new char*[argc-1];

  for (int i=1; i<argc; ++i)
  {
    char* arg = argv[i];
    int len = strlen(arg);

    // if arg starts with --
    if (len >= 3 && arg[0] == '-' && arg[1] == '-')
    {
      // --v (already handled in init)
      if (strcmp(arg, "--v") == 0)
      {
        continue;
      }

      // --Dname=value
      else if (arg[2] == 'D')
      {
        /*
        char* temp = new char[len];
        strcpy(temp, arg+3);
        char* name = strtok(temp, "=");
        char* val  = strtok(NULL, "=");
        if (val != NULL)
        {
          sysProps = setProp(sysProps, name, val);
          if (debug) printf("--   override prop %s=%s\n", name, val);
        }
        */
        continue;
      }
    }

    // pass thru to sedona
    sedonaArgv[sedonaArgc++] = arg;
  }

  if (debug)
  {
    printf("--   sedonaArgs (%d)\n", sedonaArgc);
    for (int i=0; i<sedonaArgc; ++i)
      printf("--     [%d] %s\n", i, sedonaArgv[i]);
  }

  return 0;
}

//////////////////////////////////////////////////////////////////////////
// Init Java VM
//////////////////////////////////////////////////////////////////////////

/**
 * Find the jvm.dll path to use by querying the registry.
 */
int findJvmPath()
{
  if (debug) printf("-- findJvmPath\n");

  // query registry to get current Java version
  const char* jreKey = "SOFTWARE\\JavaSoft\\Java Runtime Environment";
  char curVer[MAX_PATH];
  if (readRegistry(jreKey, "CurrentVersion", curVer, sizeof(curVer))) return -1;
  if (debug) printf("--   registry query: CurrentVersion = %s\n", curVer);

  // use curVer to get default jvm.dll to use
  char jvmKey[MAX_PATH];
  sprintf(jvmKey, "%s\\%s", jreKey, curVer);
  if (readRegistry(jvmKey, "RuntimeLib", jvmPath, sizeof(jvmPath))) return -1;
  if (debug) printf("--   registry query: RuntimeLib = %s\n", jvmPath);

  return 0;
}

//////////////////////////////////////////////////////////////////////////
// Init Options
//////////////////////////////////////////////////////////////////////////

/**
 * Get the full list of options to pass to the Java VM which
 * are the required options set by the launcher, plus any additional
 * options configured in sys.props.
 */
int initOptions()
{
  if (debug) printf("-- initOptions\n");

  // predefined classpath
  static char optClassPath[MAX_PATH];
  sprintf(optClassPath, "-Djava.class.path=%s\\lib\\sedona.jar;%s\\lib\\sedonac.jar;%s\\lib\\sedonacert.jar", sedonaHome, sedonaHome, sedonaHome);
  options[nOptions++].optionString = optClassPath;

  // predefined sedona.home
  static char optHome[MAX_PATH];
  sprintf(optHome, "-Dsedona.home=%s", sedonaHome);
  options[nOptions++].optionString = optHome;

  // debug
  if (debug)
  {
    printf("--   options:\n");
    for (int i=0; i<nOptions; ++i)
      printf("--     %s\n", options[i].optionString);
  }

  return 0;
}

//////////////////////////////////////////////////////////////////////////
// Load Java
//////////////////////////////////////////////////////////////////////////

/**
 * Load the Java VM.
 */
int loadJava()
{
  if (debug) printf("-- loadJava\n");

  // dynamically load jvm.dll
  if (debug) printf("--   load %s...\n", jvmPath);
  HINSTANCE dll = LoadLibrary(jvmPath);
  if (dll == NULL)
    return err("[launcher] Cannot load library: %s", jvmPath);

  // query for create VM procedure
  if (debug) printf("--   query procedure...\n");
  CreateJavaVMFunc createVM = (CreateJavaVMFunc)GetProcAddress(dll, "JNI_CreateJavaVM");
  if (createVM == NULL)
    return err("[launcher] Cannot find JNI_CreateJavaVM in %s", jvmPath);

  // setup args
  JavaVMInitArgs vm_args;
  vm_args.version = JNI_VERSION_1_2;
  vm_args.options = options;
  vm_args.nOptions = nOptions;
  vm_args.ignoreUnrecognized = TRUE;

  // create vm
  if (debug) printf("--   create java vm...\n");
  if (createVM(&vm, (void**)&env, &vm_args) < 0)
    return err("[launcher] Cannot launch Java VM");

  return 0;
}

//////////////////////////////////////////////////////////////////////////
// Load Java
//////////////////////////////////////////////////////////////////////////

/**
 * Find and invoke the Java runtime main.
 */
int runJavaMain()
{
  if (debug) printf("-- runJavaMain...\n");

  // figure out main
  char temp[256];
  sprintf(temp, "%s", LAUNCHER_MAIN);
  const char* mainClassName = (const char*)temp;

  // find the main class
  if (debug) printf("--   find class %s...\n", mainClassName);
  jclass mainClass = env->FindClass(mainClassName);
  if (mainClass == NULL)
    return err("[launcher] Cannot find Java main %s", mainClassName);

  // find the main method
  if (debug) printf("--   find method %s.main(String[])...\n", mainClassName);
  jmethodID mainMethod = env->GetStaticMethodID(mainClass, "main", "([Ljava/lang/String;)V");
  if (mainMethod == NULL)
    return err("[launcher] Cannot find %s.main(String[])", mainClassName);

  // map C string args to Java string args
  if (debug) printf("--   c args to java args...\n");
  jstring jstr = env->NewStringUTF("");
  jobjectArray jargs = env->NewObjectArray(sedonaArgc, env->FindClass("java/lang/String"), jstr);
  for (int i=0; i<sedonaArgc; ++i)
    env->SetObjectArrayElement(jargs, i, env->NewStringUTF(sedonaArgv[i]));

  // invoke main
  env->CallStaticVoidMethod(mainClass, mainMethod, jargs);

  return 0;
}

//////////////////////////////////////////////////////////////////////////
// Main
//////////////////////////////////////////////////////////////////////////

int main(int argc, char** argv)
{
  if (init(argc, argv)) return -1;
  if (parseArgs(argc, argv)) return -1;
  if (findJvmPath())  return -1;
  if (initOptions())  return -1;
  if (loadJava())     return -1;
  if (runJavaMain())  return -1;
  return 0;
}
