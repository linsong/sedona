//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Mar 07  Brian Frank  Creation
//

#include "sedona.h"
#include "scode.h"
#include "errorcodes.h" 

//////////////////////////////////////////////////////////////////////////
// VM Macros
//////////////////////////////////////////////////////////////////////////

#ifdef COMPUTED_GOTO
  #define EndInstr goto nextInstr
  #define Case
#else
  #define EndInstr continue
  #define Case case
#endif  

//////////////////////////////////////////////////////////////////////////
// Globals
//////////////////////////////////////////////////////////////////////////

Cell zeroCell;
Cell oneCell;
Cell negOneCell;                

//////////////////////////////////////////////////////////////////////////
// External Forwards
//////////////////////////////////////////////////////////////////////////

#ifdef SCODE_DEBUG
extern int isNativeIdValid(int kitId, int methodId);
#endif

//////////////////////////////////////////////////////////////////////////
// Internal Forwards
//////////////////////////////////////////////////////////////////////////

static int vmInit(SedonaVM* vm);   
static int vmEntry(SedonaVM* vm, int methodOffset);

//////////////////////////////////////////////////////////////////////////
// VM Entry
//////////////////////////////////////////////////////////////////////////

/* LOOPTEST
int64_t sys_Sys_ticks(SedonaVM* vm, Cell* params);
static void vmLoopTest(SedonaVM* vm)
{
  int64_t t1, t2;
  int count = 10000, i;
  vmEntry(vm, 16); // warmup cache
  t1 = sys_Sys_ticks(NULL, NULL);
  for (i=0; i<count; ++i) vmEntry(vm, 16);
  t2 = sys_Sys_ticks(NULL, NULL);
  printf("SVM %d count loops in %I64d ms\n", count, (t2-t1)/1000000i64);
}             
*/

/**
 * Initialize the Sedona VM and run the main method.
 */
int vmRun(SedonaVM* vm)
{                       
  int result;
        
  // init VM and do error checking
  result = vmInit(vm);
  if (result != 0) return result;

  // run main method
  return vmEntry(vm, 16);  // see ImageGen.java header()
}

/**
 * Resume the Sedona VM (vm must already be initialized).
 */
int vmResume(SedonaVM* vm)
{           
  // run resume method
  return vmEntry(vm, 24);  // see ImageGen.java header()
}

/**
 * Enter an initialized Sedona VM by running the method's
 * block index at the specified offset from the base of
 * code address.
 */
static int vmEntry(SedonaVM* vm, int methodOffset)
{
  int result;
  uint16_t mainBix;
  Cell args[2];      

  // init stack pointer
  vm->sp = (Cell*)vm->stackBaseAddr;
  vm->sp->ival = 0xffffffff;

  // lookup main block index
  mainBix = *(uint16_t*)(vm->codeBaseAddr+methodOffset);

  // call main with list of Str[] args
  args[0].aval = (void*)vm->args;
  args[1].ival = vm->argsLen;
  result = vm->call(vm, mainBix, args, 2); 

  return result;
}

//////////////////////////////////////////////////////////////////////////
// Init
//////////////////////////////////////////////////////////////////////////

/**
 * Initialize the VM
 */
static int vmInit(SedonaVM* vm)
{
  uint8_t* cb = (uint8_t*)vm->codeBaseAddr;
  uint32_t u4;

  // check magic (which also checks endian)
  u4 = *(uint32_t*)(cb+0);
  if (u4 != vmMagic) return ERR_BAD_IMAGE_MAGIC;

  // check version
  if (cb[4] != vmMajorVer || cb[5] != vmMinorVer) return ERR_BAD_IMAGE_VERSION;

  // check sedona block size
  if (cb[6] != SCODE_BLOCK_SIZE) return ERR_BAD_IMAGE_BLOCK_SIZE;

  // check sedona ref/pointer size
  if (cb[7] != sizeof(void*)) return ERR_BAD_IMAGE_REF_SIZE;

  // check code size
  u4 = *(uint32_t*)(cb+8);
  if (u4 != vm->codeSize) return ERR_BAD_IMAGE_CODE_SIZE;

  // initialize static data section
  u4 = *(uint32_t*)(cb+12);
  vm->dataBaseAddr = (uint8_t*)malloc(u4);
  if (vm->dataBaseAddr == NULL) return ERR_MALLOC_STATIC_DATA;
  memset(vm->dataBaseAddr, 0, u4);

  // reset assert counters
  vm->assertSuccesses = 0;
  vm->assertFailures = 0;

  // init globals
  zeroCell.ival   = 0;
  oneCell.ival    = 1;
  negOneCell.ival = -1;

  // success
  return 0;
}

/**
 * Initialize the pointers of an inline object array.  All object arrays
 * are arrays of references to keep array pointer arthimetic simple.  So
 * when an array is initialized using "{...}", we use the ArrayInit opcode
 * to setup all the pointers correctly.  Memory is laid out as such where
 * n is length:
 *
 *   ref0 = &obj0
 *   ref1 = &obj0
 *   ...
 *   refn = &objn
 *   obj0
 *   obj1
 *   ...
 *   objn
 */
static void initArray(void* base, int length, int size)
{
  void** refs   = (void**)base;
  uint8_t* objs = (uint8_t*)(refs+length);
  int i;

  for (i=0; i<length; ++i)
    refs[i] = objs + (size*i);
}

//////////////////////////////////////////////////////////////////////////
// Getters
//////////////////////////////////////////////////////////////////////////

void* getConst(SedonaVM* vm, void* self, int offset)
{
  int bix = *(uint16_t*)(((uint8_t*)self) + offset);
  return (void*)block2addr(vm->codeBaseAddr, bix);
}

uint8_t getByte(void* self, int offset)
{
  return *(((uint8_t*)self) + offset);
}

uint16_t getShort(void* self, int offset)
{
  return *(uint16_t*)(((uint8_t*)self) + offset);
}

int32_t getInt(void* self, int offset)
{
  return *(int32_t*)(((uint8_t*)self) + offset);
}

float getFloat(void* self, int offset)
{
  return *(float*)(((uint8_t*)self) + offset);
}

int64_t getWide(void* self, int offset)
{
  return *(int64_t*)(((uint8_t*)self) + offset);
}

void* getRef(void* self, int offset)
{
  return *(void**)(((uint8_t*)self) + offset);
}

void* getInline(void* self, int offset)
{
  return (void*)(((uint8_t*)self) + offset);
}

//////////////////////////////////////////////////////////////////////////
// Setters
//////////////////////////////////////////////////////////////////////////

void setByte(void* self, int offset, uint8_t val)
{
  *(((uint8_t*)self) + offset) = val;
}

void setShort(void* self, int offset, uint16_t val)
{
  *(uint16_t*)(((uint8_t*)self) + offset) = val;
}

void setInt(void* self, int offset, int32_t val)
{
  *(int32_t*)(((uint8_t*)self) + offset) = val;
}

void setFloat(void* self, int offset, float val)
{
  *(float*)(((uint8_t*)self) + offset) = val;
}

void setWide(void* self, int offset, int64_t val)
{
  *(int64_t*)(((uint8_t*)self) + offset) = val;
}

void setRef(void* self, int offset, void* val)
{
  *(void**)(((uint8_t*)self) + offset) = val;
}

//////////////////////////////////////////////////////////////////////////
// Debug
//////////////////////////////////////////////////////////////////////////

#ifdef SCODE_DEBUG

const char* qnameType(SedonaVM* vm, uint16_t block)
{
  static char buf[64];
  const uint8_t* cb = vm->codeBaseAddr;
  uint16_t* pair = (uint16_t*)block2addr(cb, block);
  const unsigned char* kitName  = block2addr(cb, pair[0]);
  const unsigned char* typeName = block2addr(cb, pair[1]);

  sprintf(buf, "%s::%s", kitName, typeName);
  return buf;
}

const char* qnameSlot(SedonaVM* vm, uint16_t block)
{
  static char buf[64];
  const uint8_t* cb = vm->codeBaseAddr;
  uint16_t* pair = (uint16_t*)block2addr(cb, block);
  const char* typeName = qnameType(vm, pair[0]);
  const unsigned char* slotName = block2addr(cb, pair[1]);

  sprintf(buf, "%s.%s", typeName, slotName);
  return buf;
}

const char* curMethod(SedonaVM* vm, Cell* fp)
{
  uint8_t* p;
  uint16_t block;

  // method address is stored +2 from frame pointer in stack
  p = (uint8_t*)(fp+2)->aval;

  // code start is +2 from method address, then skip any nops
  p += 2;
  while(*p == Nop) ++p;

  // if the first opcode is MetaSlot that is our current
  // method qname otherwise we don't have debug compiled in
  if (*p != MetaSlot) return "unknown";
  block = *(uint16_t*)(p+1);
  return qnameSlot(vm, block);
}

void dumpCallStack(SedonaVM* vm, Cell* fp)
{
  for (; fp; fp = fp[1].aval)
    printf("    %s\n", curMethod(vm, fp));
}

const char* opcodeToName(int opcode)
{
  static char temp[6];

  if (0 <= opcode && opcode < NumOpcodes)
  {
    return OpcodeNames[opcode];
  }
  else
  {
    sprintf(temp, "0x%x", opcode);
    return temp;
  }
}

void dumpStack(SedonaVM* vm, Cell* sp)
{
  Cell* base = (Cell*)vm->stackBaseAddr;
  int i = sp-base;

  for (; sp >= base; --sp, --i)
    printf("  Stack %2d [%p]:  0x%8p  %10i  %f\n", i, sp, sp->aval, sp->ival, sp->fval);
}

int handleNullPointer(SedonaVM* vm, int opcode, Cell* fp, Cell* sp)
{
  printf("ERROR: Null pointer exception\n");
  printf("  method: %s\n", curMethod(vm, fp));
  printf("  opcode: %s\n", opcodeToName(opcode));
  printf("  stack:\n");
  dumpCallStack(vm, fp);
  return ERR_NULL_POINTER;
}

int handleStackOverflow(SedonaVM* vm, int opcode, Cell* fp, Cell* sp)
{
  printf("ERROR: Stack overflow exception\n");
  printf("  method: %s\n", curMethod(vm, fp));
  printf("  opcode: %s\n", opcodeToName(opcode));
  printf("  stack:\n");
  dumpCallStack(vm, fp);
  return ERR_STACK_OVERFLOW;
}

#endif

//////////////////////////////////////////////////////////////////////////
// Loop
//////////////////////////////////////////////////////////////////////////

/**
 * This is the main loop of the VM.
 *
 * Structure of a frame as stored on the stack:
 *
 *   stack temp 2  <- sp
 *   stack temp 1
 *   stack temp 0
 *   local n       <- sp here on start of call
 *   local 1
 *   local 0       <- lp
 *   method addr
 *   prev fp
 *   return cp     <- fp
 *   param n
 *   param 1
 *   param 0       <- pp  
 *
 * Return error code in errorcodes.h on exception, otherwise return
 * the result of the method being called (it must return an int).
 */
int vmCall(SedonaVM* vm, uint16_t method, Cell* args, int argc)
{
#ifdef COMPUTED_GOTO
  static void* opcodeLabels[] = OpcodeLabelsArray;
#endif
  register Cell* sp;      // stack pointer
  register uint8_t* cp;   // code pointer
  register uint8_t* cb;   // code base
  register Cell* pp;      // param 0 pointer
  register Cell* lp;      // local 0 pointer
  register Cell* fp;      // frame pointer
  register uint8_t* db;   // static data base
  int numParams;          // number of parameter words
  int numLocals;          // number of local words
  uint16_t u2;            // temp u2
  uint8_t* addr;          // temp pointer
  Cell cell;              // temp stack cell
  NativeMethod native;    // temp native method func pointer
  int64_t s8;             // temp signed 64-bit long      
  Cell* maxStackAddr;     // pointer to top of stack area
  NativeMethod** nativeTable;  // cached pointer to native table
                
  // init pointers
  sp = vm->sp;
  cb = (uint8_t*)vm->codeBaseAddr;
  db = vm->dataBaseAddr;
  cp = block2addr(cb, method);
  numParams = cp[0];
  numLocals = cp[1];                                     
  maxStackAddr = (Cell*)(vm->stackBaseAddr + vm->stackMaxSize);
  nativeTable = vm->nativeTable;


  // check that method has matching parameters
  // and push arguments onto the stack
  if (numParams != argc) return ERR_INVALID_METHOD_PARAMS;
  for (u2 = 0; u2 < argc; ++u2) *(++sp) = args[u2];

  // setup for first call to method
  (++sp)->aval = 0;    // push return cp of zero
  (++sp)->aval = 0;    // push prev fp of zero
  (++sp)->aval = cp;   // push method address
  fp = sp-2;           // init framepointer
  pp = fp-numParams;   // update param 0 pointer
  lp = fp+3;           // update local 0 pointer
  sp += numLocals;     // make room for locals on stack
  cp += 2;             // advance to first opcode

#ifdef COMPUTED_GOTO
  // lookup next label for instruction and jump using computed goto;
  // this optimization is only available for GCC, however it can shave
  // off a few machine instructions for each opcode (see sedona.h)
  nextInstr: goto *opcodeLabels[*cp];
#else       
  // loop forever if not using computed gotos
  for (;;)
  { 
#endif

    // if debug
    #ifdef SCODE_DEBUG    
      // check for null pointer           
      int offset = OpcodePointerOffsets[*cp];
      if (offset >= 0 && ((sp-offset)->aval) == NULL)
        return handleNullPointer(vm, *cp, fp, sp);
        
      // check for stack overflow           
      if (sp >= maxStackAddr)
        return handleStackOverflow(vm, *cp, fp, sp);
        
    #endif

    //dumpStack(vm, sp);          
    //printf("  -- opcode = [%d]  %s\n", cp-cb, opcodeToName(*cp));

#ifndef COMPUTED_GOTO
    // process next opcode using switch if not using computed gotos
    switch (*cp)
    {
#endif
      ////////////////////////////////////////////////////////////////////
      // literals
      ////////////////////////////////////////////////////////////////////

      Case Nop:           ++cp; EndInstr;
      Case LoadIM1:       (++sp)->ival = -1; ++cp; EndInstr;
      Case LoadNull:   
      Case LoadI0:        (++sp)->ival = 0;  ++cp; EndInstr;
      Case LoadI1:        (++sp)->ival = 1;  ++cp; EndInstr;
      Case LoadNullBool:  
      Case LoadI2:        (++sp)->ival = 2;  ++cp; EndInstr;
      Case LoadI3:        (++sp)->ival = 3;  ++cp; EndInstr;
      Case LoadI4:        (++sp)->ival = 4;  ++cp; EndInstr;
      Case LoadI5:        (++sp)->ival = 5;  ++cp; EndInstr;
      Case LoadIntU1:     (++sp)->ival = *(cp+1);            cp += 2; EndInstr;
      Case LoadIntU2:     (++sp)->ival = *(uint16_t*)(cp+1); cp += 3; EndInstr;
      Case LoadL0:        ++sp; *((int64_t*)sp) = 0; ++sp; ++cp; EndInstr;
      Case LoadL1:        ++sp; *((int64_t*)sp) = 1; ++sp; ++cp; EndInstr;
      Case LoadF0:        (++sp)->fval = 0.0f; ++cp; EndInstr;
      Case LoadF1:        (++sp)->fval = 1.0f; ++cp; EndInstr;
      Case LoadNullFloat: (++sp)->ival = NULLFLOAT; ++cp; EndInstr;
      Case LoadNullDouble: ++sp; *((uint64_t*)sp) = NULLDOUBLE; ++sp; ++cp; EndInstr;
      Case LoadD0:        ++sp; *((double*)sp) = 0.0; ++sp; ++cp; EndInstr;
      Case LoadD1:        ++sp; *((double*)sp) = 1.0; ++sp; ++cp; EndInstr;      
      Case LoadInt:
      Case LoadFloat:     u2 = *(uint16_t*)(cp+1); (++sp)->ival = *(int32_t*)block2addr(cb, u2); cp += 3; EndInstr;
      Case LoadLong:
      Case LoadDouble:    u2 = *(uint16_t*)(cp+1); sp +=2 ; *(int64_t*)(sp-1) = *(int64_t*)block2addr(cb, u2); cp += 3; EndInstr;        
      Case LoadStr:       
      Case LoadBuf:
      Case LoadType:
      Case LoadSlot:      u2 = *(uint16_t*)(cp+1); (++sp)->aval = block2addr(cb, u2); cp += 3; EndInstr;

      ////////////////////////////////////////////////////////////////////
      // params
      ////////////////////////////////////////////////////////////////////

      // parm load
      Case LoadParam0:     *(++sp) = *(pp);   ++cp; EndInstr;
      Case LoadParam1:     *(++sp) = *(pp+1); ++cp; EndInstr;
      Case LoadParam2:     *(++sp) = *(pp+2); ++cp; EndInstr;
      Case LoadParam3:     *(++sp) = *(pp+3); ++cp; EndInstr;
      Case LoadParam:      *(++sp) = *(pp + *(cp+1)); cp += 2; EndInstr;
      Case LoadParamWide:  *(int64_t*)(++sp) = *(int64_t*)(pp + *(cp+1)); ++sp; cp += 2; EndInstr;

      // param store
      Case StoreParam:     *(pp + *(cp+1)) = *sp--; cp += 2; EndInstr;
      Case StoreParamWide: *(int64_t*)(pp + *(cp+1)) = *(int64_t*)(sp-1); sp -=2; cp += 2; EndInstr;

      ////////////////////////////////////////////////////////////////////
      // locals
      ////////////////////////////////////////////////////////////////////

      // load local
      Case LoadLocal0:     *(++sp) = *(lp);   ++cp; EndInstr;
      Case LoadLocal1:     *(++sp) = *(lp+1); ++cp; EndInstr;
      Case LoadLocal2:     *(++sp) = *(lp+2); ++cp; EndInstr;
      Case LoadLocal3:     *(++sp) = *(lp+3); ++cp; EndInstr;
      Case LoadLocal4:     *(++sp) = *(lp+4); ++cp; EndInstr;
      Case LoadLocal5:     *(++sp) = *(lp+5); ++cp; EndInstr;
      Case LoadLocal6:     *(++sp) = *(lp+6); ++cp; EndInstr;
      Case LoadLocal7:     *(++sp) = *(lp+7); ++cp; EndInstr;
      Case LoadLocal:      *(++sp) = *(lp + *(cp+1)); cp += 2; EndInstr;
      Case LoadLocalWide:  *(int64_t*)(++sp) = *(int64_t*)(lp + *(cp+1)); ++sp; cp += 2; EndInstr;

      // store local
      Case StoreLocal0:    *lp = *sp--;      ++cp; EndInstr;
      Case StoreLocal1:    *(lp+1) = *sp--;  ++cp; EndInstr;
      Case StoreLocal2:    *(lp+2) = *sp--;  ++cp; EndInstr;
      Case StoreLocal3:    *(lp+3) = *sp--;  ++cp; EndInstr;
      Case StoreLocal4:    *(lp+4) = *sp--;  ++cp; EndInstr;
      Case StoreLocal5:    *(lp+5) = *sp--;  ++cp; EndInstr;
      Case StoreLocal6:    *(lp+6) = *sp--;  ++cp; EndInstr;
      Case StoreLocal7:    *(lp+7) = *sp--;  ++cp; EndInstr;
      Case StoreLocal:     *(lp + *(cp+1)) = *sp--; cp += 2; EndInstr;
      Case StoreLocalWide: *(int64_t*)(lp + *(cp+1)) = *(int64_t*)(sp-1); sp -=2; cp += 2; EndInstr;

      ////////////////////////////////////////////////////////////////////
      // int
      ////////////////////////////////////////////////////////////////////

      // int compare
      Case IntEq:
//if (sp->ival != (sp+1)->ival) printf("Not equal %d ?= %d\n", (sp-1)->ival, (sp)->ival);
          --sp; sp->ival = sp->ival == (sp+1)->ival; ++cp; EndInstr;
      Case IntNotEq: --sp; sp->ival = sp->ival != (sp+1)->ival; ++cp; EndInstr;
      Case IntGt:    --sp; sp->ival = sp->ival >  (sp+1)->ival; ++cp; EndInstr;
      Case IntGtEq:  --sp; sp->ival = sp->ival >= (sp+1)->ival; ++cp; EndInstr;
      Case IntLt:    --sp; sp->ival = sp->ival <  (sp+1)->ival; ++cp; EndInstr;
      Case IntLtEq:  --sp; sp->ival = sp->ival <= (sp+1)->ival; ++cp; EndInstr;

      // int math
      Case IntMul:    --sp; sp->ival = sp->ival * (sp+1)->ival; ++cp; EndInstr;
      Case IntDiv:    --sp; sp->ival = sp->ival / (sp+1)->ival; ++cp; EndInstr;
      Case IntMod:    --sp; sp->ival = sp->ival % (sp+1)->ival; ++cp; EndInstr;
      Case IntAdd:    --sp; sp->ival = sp->ival + (sp+1)->ival; ++cp; EndInstr;
      Case IntSub:    --sp; sp->ival = sp->ival - (sp+1)->ival; ++cp; EndInstr;
      Case IntOr:     --sp; sp->ival = sp->ival | (sp+1)->ival; ++cp; EndInstr;
      Case IntXor:    --sp; sp->ival = sp->ival ^ (sp+1)->ival; ++cp; EndInstr;
      Case IntAnd:    --sp; sp->ival = sp->ival & (sp+1)->ival; ++cp; EndInstr;
      Case IntNot:    sp->ival = ~sp->ival; ++cp; EndInstr;
      Case IntNeg:    sp->ival = -sp->ival; ++cp; EndInstr;
      Case IntShiftL: --sp; sp->ival = sp->ival << (sp+1)->ival; ++cp; EndInstr;
      Case IntShiftR: --sp; sp->ival = sp->ival >> (sp+1)->ival; ++cp; EndInstr;
      Case IntInc:    ++sp->ival; ++cp; EndInstr;
      Case IntDec:    --sp->ival; ++cp; EndInstr;

      ////////////////////////////////////////////////////////////////////
      // long
      ////////////////////////////////////////////////////////////////////

      // long compare
      Case LongEq:    sp -=3;  sp->ival = *(int64_t*)sp == *(int64_t*)(sp+2); ++cp;  EndInstr;
      Case LongNotEq: sp -=3;  sp->ival = *(int64_t*)sp != *(int64_t*)(sp+2); ++cp;  EndInstr;
      Case LongGt:    sp -=3;  sp->ival = *(int64_t*)sp >  *(int64_t*)(sp+2); ++cp;  EndInstr;
      Case LongGtEq:  sp -=3;  sp->ival = *(int64_t*)sp >= *(int64_t*)(sp+2); ++cp;  EndInstr;
      Case LongLt:    sp -=3;  sp->ival = *(int64_t*)sp <  *(int64_t*)(sp+2); ++cp;  EndInstr;
      Case LongLtEq:  sp -=3;  sp->ival = *(int64_t*)sp <= *(int64_t*)(sp+2); ++cp;  EndInstr;

      // long math
      Case LongMul:  sp -=2; *(int64_t*)(sp-1) = *(int64_t*)(sp-1) * *(int64_t*)(sp+1); ++cp; EndInstr;
      Case LongDiv:  sp -=2; *(int64_t*)(sp-1) = *(int64_t*)(sp-1) / *(int64_t*)(sp+1); ++cp; EndInstr;   // divdi3
      Case LongMod:  sp -=2; *(int64_t*)(sp-1) = *(int64_t*)(sp-1) % *(int64_t*)(sp+1); ++cp; EndInstr;   // moddi3
      Case LongAdd:  sp -=2; *(int64_t*)(sp-1) = *(int64_t*)(sp-1) + *(int64_t*)(sp+1); ++cp; EndInstr;
      Case LongSub:  sp -=2; *(int64_t*)(sp-1) = *(int64_t*)(sp-1) - *(int64_t*)(sp+1); ++cp; EndInstr;
      Case LongOr:   sp -=2; *(int64_t*)(sp-1) = *(int64_t*)(sp-1) | *(int64_t*)(sp+1); ++cp; EndInstr;
      Case LongXor:  sp -=2; *(int64_t*)(sp-1) = *(int64_t*)(sp-1) ^ *(int64_t*)(sp+1); ++cp; EndInstr;
      Case LongAnd:  sp -=2; *(int64_t*)(sp-1) = *(int64_t*)(sp-1) & *(int64_t*)(sp+1); ++cp; EndInstr;
      Case LongNot:  *(int64_t*)(sp-1) = ~(*(int64_t*)(sp-1)); ++cp; EndInstr;
      Case LongNeg:  *(int64_t*)(sp-1) = -(*(int64_t*)(sp-1)); ++cp; EndInstr;
      Case LongShiftL: sp -= 1; *(int64_t*)(sp-1) = *(int64_t*)(sp-1) << (sp+1)->ival; ++cp; EndInstr;
      Case LongShiftR: sp -= 1; *(int64_t*)(sp-1) = *(int64_t*)(sp-1) >> (sp+1)->ival; ++cp; EndInstr;

      ////////////////////////////////////////////////////////////////////
      // float
      ////////////////////////////////////////////////////////////////////
      Case FloatEq:    
        --sp; 
        if (ISNANF(sp->fval) && ISNANF((sp+1)->fval))   // special case for Sedona
          sp->ival = TRUE;
        else
          sp->ival = sp->fval == (sp+1)->fval;          // regular float comparison
        ++cp; 
        EndInstr;
      Case FloatNotEq: 
        --sp; 
        if (ISNANF(sp->fval) && ISNANF((sp+1)->fval))   // special case for Sedona
          sp->ival = FALSE;
        else
          sp->ival = sp->fval != (sp+1)->fval;          // regular float comparison
        ++cp; 
        EndInstr;
      ////////////////////////////////////////////////////////////////////
      //
      Case FloatGt:    --sp; sp->ival = sp->fval >  (sp+1)->fval; ++cp; EndInstr;
      Case FloatGtEq:  --sp; sp->ival = sp->fval >= (sp+1)->fval; ++cp; EndInstr;
      Case FloatLt:    --sp; sp->ival = sp->fval <  (sp+1)->fval; ++cp; EndInstr;
      Case FloatLtEq:  --sp; sp->ival = sp->fval <= (sp+1)->fval; ++cp; EndInstr;

      // float math
      Case FloatMul:    --sp; sp->fval = sp->fval * (sp+1)->fval; ++cp; EndInstr;
      Case FloatDiv:    --sp; sp->fval = sp->fval / (sp+1)->fval; ++cp; EndInstr;
      Case FloatAdd:    --sp; sp->fval = sp->fval + (sp+1)->fval; ++cp; EndInstr;
      Case FloatSub:    --sp; sp->fval = sp->fval - (sp+1)->fval; ++cp; EndInstr;
      Case FloatNeg:    sp->fval = -sp->fval; ++cp; EndInstr;

      ////////////////////////////////////////////////////////////////////
      // double
      ////////////////////////////////////////////////////////////////////
      Case DoubleEq:    
        sp -= 3; 
        if (ISNAN(*(double*)sp) && ISNAN(*(double*)(sp+2)))   // special case for Sedona
          sp->ival = TRUE;
        else
          sp->ival = *(double*)sp == *(double*)(sp+2);        // regular double comparison
        ++cp;  
        EndInstr;
      Case DoubleNotEq: 
        sp -= 3; 
        if (ISNAN(*(double*)sp) && ISNAN(*(double*)(sp+2)))   // special case for Sedona
          sp->ival = FALSE;
        else
          sp->ival = *(double*)sp != *(double*)(sp+2);        // regular double comparison
        ++cp;  
        EndInstr;
      ////////////////////////////////////////////////////////////////////
      //
      Case DoubleGt:    sp -= 3; sp->ival = *(double*)sp >  *(double*)(sp+2); ++cp;  EndInstr;
      Case DoubleGtEq:  sp -= 3; sp->ival = *(double*)sp >= *(double*)(sp+2); ++cp;  EndInstr;
      Case DoubleLt:    sp -= 3; sp->ival = *(double*)sp <  *(double*)(sp+2); ++cp;  EndInstr;
      Case DoubleLtEq:  sp -= 3; sp->ival = *(double*)sp <= *(double*)(sp+2); ++cp;  EndInstr;

      // double math
      Case DoubleMul:  sp -=2; *(double*)(sp-1) = *(double*)(sp-1) * *(double*)(sp+1); ++cp; EndInstr;
      Case DoubleDiv:  sp -=2; *(double*)(sp-1) = *(double*)(sp-1) / *(double*)(sp+1); ++cp; EndInstr;
      Case DoubleAdd:  sp -=2; *(double*)(sp-1) = *(double*)(sp-1) + *(double*)(sp+1); ++cp; EndInstr;
      Case DoubleSub:  sp -=2; *(double*)(sp-1) = *(double*)(sp-1) - *(double*)(sp+1); ++cp; EndInstr;
      Case DoubleNeg:  *(double*)(sp-1) = -(*(double*)(sp-1)); ++cp; EndInstr;
                                                                                        
      ////////////////////////////////////////////////////////////////////
      // obj
      ////////////////////////////////////////////////////////////////////

      // obj compare
      Case ObjEq:        --sp; sp->ival = sp->aval == (sp+1)->aval; ++cp; EndInstr;
      Case ObjNotEq:     --sp; sp->ival = sp->aval != (sp+1)->aval; ++cp; EndInstr;
      
      ////////////////////////////////////////////////////////////////////
      // general purpose
      ////////////////////////////////////////////////////////////////////

      Case EqZero:    sp->ival = sp->ival == 0; ++cp; EndInstr;
      Case NotEqZero: sp->ival = sp->ival != 0; ++cp; EndInstr;
      
      ////////////////////////////////////////////////////////////////////
      // casting
      ////////////////////////////////////////////////////////////////////

      Case LongToInt:   --sp; sp->ival = (int)(*(int64_t*)sp); ++cp; EndInstr;
      Case FloatToInt:        sp->ival = (int)(sp->fval);      ++cp; EndInstr;
      Case DoubleToInt: --sp; sp->ival = (int)(*(double*)sp);  ++cp; EndInstr;

      Case IntToLong:     *(int64_t*)sp     = (int64_t)(sp->ival); ++sp;   ++cp; EndInstr;
      Case FloatToLong:   *(int64_t*)sp     = (int64_t)(sp->fval); ++sp;   ++cp; EndInstr;
      Case DoubleToLong:  *(int64_t*)(sp-1) = (int64_t)(*(double*)(sp-1)); ++cp; EndInstr;

      Case IntToFloat:          sp->fval = (float)(sp->ival);      ++cp; EndInstr;
      Case LongToFloat:   --sp; sp->fval = (float)(*(int64_t*)sp); ++cp; EndInstr;
      Case DoubleToFloat: --sp; sp->fval = (float)(*(double*)sp);  ++cp; EndInstr;

      Case IntToDouble:     *(double*)sp     = (double)(sp->ival); ++sp;    ++cp; EndInstr;
      Case LongToDouble:    *(double*)(sp-1) = (double)(*(int64_t*)(sp-1)); ++cp; EndInstr;
      Case FloatToDouble:   *(double*)sp     = (double)(sp->fval); ++sp;    ++cp; EndInstr;
      
      ////////////////////////////////////////////////////////////////////
      // stack manipulation
      ////////////////////////////////////////////////////////////////////

      Case Dup:  ++sp; *sp = *(sp-1);  ++cp; EndInstr;
      Case Dup2: sp += 2; *sp = *(sp-2); *(sp-1) = *(sp-3); ++cp; EndInstr;
      Case DupDown2: ++sp; *sp = *(sp-1); *(sp-1) = *(sp-2); *(sp-2) = *sp; ++cp; EndInstr;
      Case DupDown3: ++sp; *sp = *(sp-1); *(sp-1) = *(sp-2); *(sp-2) = *(sp-3); *(sp-3) = *sp; ++cp; EndInstr;      
      Case Dup2Down2: sp += 2; *sp = *(sp-2); *(sp-1) = *(sp-3); *(sp-2) = *(sp-4); *(sp-3) = *sp; *(sp-4) = *(sp-1); ++cp; EndInstr;
      Case Dup2Down3: sp += 2; *sp = *(sp-2); *(sp-1) = *(sp-3); *(sp-2) = *(sp-4); *(sp-3) = *(sp-5); *(sp-4) = *sp; *(sp-5) = *(sp-1); ++cp; EndInstr;
        
      Case Pop:  --sp; ++cp; EndInstr;
      Case Pop2: sp -= 2; ++cp; EndInstr;
      Case Pop3: sp -= 3; ++cp; EndInstr;

      ////////////////////////////////////////////////////////////////////
      // near jumps 
      ////////////////////////////////////////////////////////////////////

      Case Jump:
        cp += *(int8_t*)(cp+1);
        EndInstr;

      Case JumpZero:
        if (!sp->ival)
          cp += *(int8_t*)(cp+1);
        else
          cp += 2;
        --sp;
        EndInstr;

      Case JumpNonZero:
        if (sp->ival)
          cp += *(int8_t*)(cp+1);
        else
          cp += 2;
        --sp;
        EndInstr;

      Case Foreach:
        if (++(sp->ival) >= (sp-1)->ival)
        {
          cp += *(int8_t*)(cp+1);
        }
        else
        {
          // push array, counter onto stack; the compiler
          // emits the LoadxxxArray instruction for array type
          ++sp; sp->aval = (sp-3)->aval;
          ++sp; sp->ival = (sp-2)->ival;
          cp += 2;
        }
        EndInstr;

      ////////////////////////////////////////////////////////////////////
      // far jumps 
      ////////////////////////////////////////////////////////////////////

      Case JumpFar:
        cp += *(int16_t*)(cp+1);
        EndInstr;

      Case JumpFarZero:
        if (!sp->ival)
          cp += *(int16_t*)(cp+1);
        else
          cp += 3;
        --sp;
        EndInstr;

      Case JumpFarNonZero:
        if (sp->ival)
          cp += *(int16_t*)(cp+1);
        else
          cp += 3;
        --sp;
        EndInstr;

      Case ForeachFar:
        if (++(sp->ival) >= (sp-1)->ival)
        {
          cp += *(int16_t*)(cp+1);
        }
        else
        {
          // push array, counter onto stack; the compiler
          // emits the LoadxxxArray instruction for array type
          ++sp; sp->aval = (sp-3)->aval;
          ++sp; sp->ival = (sp-2)->ival;
          cp += 3;
        }
        EndInstr;

      ////////////////////////////////////////////////////////////////////
      // int compare near jumps
      ////////////////////////////////////////////////////////////////////

      Case JumpIntEq:                                                          
        if ((sp-1)->ival == sp->ival)
          cp += *(int8_t*)(cp+1);
        else
          cp += 2;
        sp -= 2;
        EndInstr;

      Case JumpIntNotEq:                                                          
        if ((sp-1)->ival != sp->ival)
          cp += *(int8_t*)(cp+1);
        else
          cp += 2;
        sp -= 2;
        EndInstr;

      Case JumpIntGt:                                                          
        if ((sp-1)->ival > sp->ival)
          cp += *(int8_t*)(cp+1);
        else
          cp += 2;
        sp -= 2;
        EndInstr;

      Case JumpIntGtEq:                                                          
        if ((sp-1)->ival >= sp->ival)
          cp += *(int8_t*)(cp+1);
        else
          cp += 2;
        sp -= 2;
        EndInstr;

      Case JumpIntLt:                                                          
        if ((sp-1)->ival < sp->ival)
          cp += *(int8_t*)(cp+1);
        else
          cp += 2;
        sp -= 2;
        EndInstr;

      Case JumpIntLtEq:                                                          
        if ((sp-1)->ival <= sp->ival)
          cp += *(int8_t*)(cp+1);
        else
          cp += 2;
        sp -= 2;
        EndInstr;

      ////////////////////////////////////////////////////////////////////
      // int compare far jumps
      ////////////////////////////////////////////////////////////////////

      Case JumpFarIntEq:                                                          
        if ((sp-1)->ival == sp->ival)
          cp += *(int16_t*)(cp+1);
        else
          cp += 3;
        sp -= 2;
        EndInstr;

      Case JumpFarIntNotEq:                                                          
        if ((sp-1)->ival != sp->ival)
          cp += *(int16_t*)(cp+1);
        else
          cp += 3;
        sp -= 2;
        EndInstr;

      Case JumpFarIntGt:                                                          
        if ((sp-1)->ival > sp->ival)
          cp += *(int16_t*)(cp+1);
        else
          cp += 3;
        sp -= 2;
        EndInstr;

      Case JumpFarIntGtEq:                                                          
        if ((sp-1)->ival >= sp->ival)
          cp += *(int16_t*)(cp+1);
        else
          cp += 3;
        sp -= 2;
        EndInstr;

      Case JumpFarIntLt:                                                          
        if ((sp-1)->ival < sp->ival)
          cp += *(int16_t*)(cp+1);
        else
          cp += 3;
        sp -= 2;
        EndInstr;

      Case JumpFarIntLtEq:                                                          
        if ((sp-1)->ival <= sp->ival)
          cp += *(int16_t*)(cp+1);
        else
          cp += 3;
        sp -= 2;
        EndInstr;

      ////////////////////////////////////////////////////////////////////
      // storage
      ////////////////////////////////////////////////////////////////////

      Case LoadDataAddr:  (++sp)->aval = db; ++cp; EndInstr;

      // load 8-bit fields
      Case Load8BitFieldU1:   sp->ival = *(uint8_t*)(((uint8_t*)sp->aval) + *(uint8_t *)(cp+1)); cp += 2; EndInstr;
      Case Load8BitFieldU2:   sp->ival = *(uint8_t*)(((uint8_t*)sp->aval) + *(uint16_t*)(cp+1)); cp += 3; EndInstr;
      Case Load8BitFieldU4:   sp->ival = *(uint8_t*)(((uint8_t*)sp->aval) + *(uint32_t*)(cp+1)); cp += 5; EndInstr;
      Case Load8BitArray:     --sp; sp->ival = *(((uint8_t*)sp->aval) + (sp+1)->ival); ++cp; EndInstr;
      Case Add8BitArray:      --sp; sp->aval = (((uint8_t*)sp->aval) + (sp+1)->ival); ++cp; EndInstr;

      // store 8-bit fields
      Case Store8BitFieldU1:  *(uint8_t*)(((uint8_t*)((sp-1)->aval)) + *(uint8_t *)(cp+1)) = sp->ival; sp -= 2; cp += 2; EndInstr;
      Case Store8BitFieldU2:  *(uint8_t*)(((uint8_t*)((sp-1)->aval)) + *(uint16_t*)(cp+1)) = sp->ival; sp -= 2; cp += 3; EndInstr;
      Case Store8BitFieldU4:  *(uint8_t*)(((uint8_t*)((sp-1)->aval)) + *(uint32_t*)(cp+1)) = sp->ival; sp -= 2; cp += 5; EndInstr;
      Case Store8BitArray:    *(((uint8_t*)((sp-2)->aval)) + ((sp-1)->ival)) = sp->ival; sp -= 3; ++cp; EndInstr;

      // load 16-bit fields
      Case Load16BitFieldU1:  sp->ival = *(uint16_t*)(((uint8_t*)sp->aval) + *(uint8_t *)(cp+1)); cp += 2; EndInstr;
      Case Load16BitFieldU2:  sp->ival = *(uint16_t*)(((uint8_t*)sp->aval) + *(uint16_t*)(cp+1)); cp += 3; EndInstr;
      Case Load16BitFieldU4:  sp->ival = *(uint16_t*)(((uint8_t*)sp->aval) + *(uint32_t*)(cp+1)); cp += 5; EndInstr;
      Case Load16BitArray:    --sp; sp->ival = *(((uint16_t*)sp->aval) + (sp+1)->ival); ++cp; EndInstr;
      Case Add16BitArray:     --sp; sp->aval = (((uint16_t*)sp->aval) + (sp+1)->ival); ++cp; EndInstr;

      // store 16-bit fields
      Case Store16BitFieldU1: *(uint16_t*)(((uint8_t*)((sp-1)->aval)) + *(uint8_t *)(cp+1)) = sp->ival; sp -= 2; cp += 2; EndInstr;
      Case Store16BitFieldU2: *(uint16_t*)(((uint8_t*)((sp-1)->aval)) + *(uint16_t*)(cp+1)) = sp->ival; sp -= 2; cp += 3; EndInstr;
      Case Store16BitFieldU4: *(uint16_t*)(((uint8_t*)((sp-1)->aval)) + *(uint32_t*)(cp+1)) = sp->ival; sp -= 2; cp += 5; EndInstr;
      Case Store16BitArray:   *(((uint16_t*)((sp-2)->aval)) + ((sp-1)->ival)) = sp->ival; sp -= 3; ++cp; EndInstr;

      // load 32-bit fields
      Case Load32BitFieldU1:  sp->ival = *(int32_t*)(((uint8_t*)sp->aval) + *(uint8_t *)(cp+1)); cp += 2; EndInstr;
      Case Load32BitFieldU2:  sp->ival = *(int32_t*)(((uint8_t*)sp->aval) + *(uint16_t*)(cp+1)); cp += 3; EndInstr;
      Case Load32BitFieldU4:  sp->ival = *(int32_t*)(((uint8_t*)sp->aval) + *(uint32_t*)(cp+1)); cp += 5; EndInstr;
      Case Load32BitArray:    --sp; sp->ival = *(((int32_t*)sp->aval) + (sp+1)->ival); ++cp; EndInstr;
      Case Add32BitArray:     --sp; sp->aval = (((int32_t*)sp->aval) + (sp+1)->ival); ++cp; EndInstr;

      // store 32-bit fields
      Case Store32BitFieldU1: *(int32_t*)(((uint8_t*)((sp-1)->aval)) + *(uint8_t *)(cp+1)) = sp->ival; sp -= 2; cp += 2; EndInstr;
      Case Store32BitFieldU2: *(int32_t*)(((uint8_t*)((sp-1)->aval)) + *(uint16_t*)(cp+1)) = sp->ival; sp -= 2; cp += 3; EndInstr;
      Case Store32BitFieldU4: *(int32_t*)(((uint8_t*)((sp-1)->aval)) + *(uint32_t*)(cp+1)) = sp->ival; sp -= 2; cp += 5; EndInstr;
      Case Store32BitArray:   *(((int32_t*)((sp-2)->aval)) + ((sp-1)->ival)) = sp->ival; sp -= 3; ++cp; EndInstr;

      // load 64-bit fields
      Case Load64BitFieldU1:  *(int64_t*)sp = *(int64_t*)(((uint8_t*)sp->aval) + *(uint8_t *)(cp+1)); ++sp; cp += 2; EndInstr;
      Case Load64BitFieldU2:  *(int64_t*)sp = *(int64_t*)(((uint8_t*)sp->aval) + *(uint16_t*)(cp+1)); ++sp; cp += 3; EndInstr;
      Case Load64BitFieldU4:  *(int64_t*)sp = *(int64_t*)(((uint8_t*)sp->aval) + *(uint32_t*)(cp+1)); ++sp; cp += 5; EndInstr;
      Case Load64BitArray:    --sp; *(int64_t*)sp = *(((int64_t*)sp->aval) + (sp+1)->ival); ++cp; ++sp; EndInstr;
      Case Add64BitArray:     --sp; sp->aval = (((int64_t*)sp->aval) + (sp+1)->ival); ++cp; EndInstr;

      // store 64-bit fields
      Case Store64BitFieldU1: *(int64_t*)(((uint8_t*)((sp-2)->aval)) + *(uint8_t *)(cp+1)) = *(int64_t*)(sp-1); sp -= 3; cp += 2; EndInstr;
      Case Store64BitFieldU2: *(int64_t*)(((uint8_t*)((sp-2)->aval)) + *(uint16_t*)(cp+1)) = *(int64_t*)(sp-1); sp -= 3; cp += 3; EndInstr;
      Case Store64BitFieldU4: *(int64_t*)(((uint8_t*)((sp-2)->aval)) + *(uint32_t*)(cp+1)) = *(int64_t*)(sp-1); sp -= 3; cp += 5; EndInstr;     
      Case Store64BitArray:   *(((int64_t*)((sp-3)->aval)) + ((sp-2)->ival)) = *(int64_t*)(sp-1); sp -= 4; ++cp; EndInstr;

      // load ref fields (variable width based on pointer size)
      Case LoadRefFieldU1:  sp->aval = *(void**)(((uint8_t*)sp->aval) + *(uint8_t *)(cp+1)); cp += 2; EndInstr;
      Case LoadRefFieldU2:  sp->aval = *(void**)(((uint8_t*)sp->aval) + *(uint16_t*)(cp+1)); cp += 3; EndInstr;
      Case LoadRefFieldU4:  sp->aval = *(void**)(((uint8_t*)sp->aval) + *(uint32_t*)(cp+1)); cp += 5; EndInstr;
      Case LoadRefArray:    --sp; sp->aval = *(((void**)sp->aval) + (sp+1)->ival); ++cp; EndInstr;
      Case AddRefArray:     --sp; sp->aval = (((void**)sp->aval) + (sp+1)->ival); ++cp; EndInstr;

      // load const fields (block index into scode section)
      Case LoadConstFieldU1: u2 = *(uint16_t*)(((uint8_t*)sp->aval) + *(uint8_t *)(cp+1)); sp->aval = u2 ? block2addr(cb, u2) : NULL; cp += 2; EndInstr;
      Case LoadConstFieldU2: u2 = *(uint16_t*)(((uint8_t*)sp->aval) + *(uint16_t*)(cp+1)); sp->aval = u2 ? block2addr(cb, u2) : NULL; cp += 3; EndInstr;
      Case LoadConstArray:   --sp; u2 = *(((uint16_t*)sp->aval) + (sp+1)->ival); sp->aval = u2 ? block2addr(cb, u2) : NULL; ++cp; EndInstr;
      Case LoadConstStatic:  u2 = *(uint16_t*)(cp+1); (++sp)->aval = u2 ? block2addr(cb, u2) : NULL; cp += 3; EndInstr;

      // store ref fields (variable width based on pointer size)
      Case StoreRefFieldU1: *(void**)(((uint8_t*)((sp-1)->aval)) + *(uint8_t *)(cp+1)) = sp->aval; sp -= 2; cp += 2; EndInstr;
      Case StoreRefFieldU2: *(void**)(((uint8_t*)((sp-1)->aval)) + *(uint16_t*)(cp+1)) = sp->aval; sp -= 2; cp += 3; EndInstr;
      Case StoreRefFieldU4: *(void**)(((uint8_t*)((sp-1)->aval)) + *(uint32_t*)(cp+1)) = sp->aval; sp -= 2; cp += 5; EndInstr;
      Case StoreRefArray:   *(((void**)((sp-2)->aval)) + ((sp-1)->ival)) = sp->aval; sp -= 3; ++cp; EndInstr;

      // load inline
      Case LoadInlineFieldU1: sp->aval = (void*)(((uint8_t*)sp->aval) + *(uint8_t *)(cp+1)); cp += 2; EndInstr;
      Case LoadInlineFieldU2: sp->aval = (void*)(((uint8_t*)sp->aval) + *(uint16_t*)(cp+1)); cp += 3; EndInstr;
      Case LoadInlineFieldU4: sp->aval = (void*)(((uint8_t*)sp->aval) + *(uint32_t*)(cp+1)); cp += 5; EndInstr;

      // load param0 inline
      Case LoadParam0InlineFieldU1: (++sp)->aval = (void*)((uint8_t*)pp->aval + *(uint8_t *)(cp+1)); cp += 2; EndInstr;
      Case LoadParam0InlineFieldU2: (++sp)->aval = (void*)((uint8_t*)pp->aval + *(uint16_t*)(cp+1)); cp += 3; EndInstr;
      Case LoadParam0InlineFieldU4: (++sp)->aval = (void*)((uint8_t*)pp->aval + *(uint32_t*)(cp+1)); cp += 5; EndInstr;

      // load static inline
      Case LoadDataInlineFieldU1: (++sp)->aval = (void*)(db + *(uint8_t *)(cp+1)); cp += 2; EndInstr;
      Case LoadDataInlineFieldU2: (++sp)->aval = (void*)(db + *(uint16_t*)(cp+1)); cp += 3; EndInstr;
      Case LoadDataInlineFieldU4: (++sp)->aval = (void*)(db + *(uint32_t*)(cp+1)); cp += 5; EndInstr;

      ////////////////////////////////////////////////////////////////////
      // method calling
      ////////////////////////////////////////////////////////////////////

      Case LoadParam0Call:
        *(++sp) = *(pp);
        // fall thru

      Case Call:
        // non-virtual call
#ifdef IS_BIG_ENDIAN        
        u2 = *(cp+1);              // get block index (unaligned, big endian)
        u2 = (u2 << 8) | *(cp+2);
#else        
        u2 = *(cp+2);              // get block index (unaligned, little endian)
        u2 = (u2 << 8) | *(cp+1);
#endif        
        addr = block2addr(cb, u2); // address of target method
        (++sp)->aval = cp+3;       // push return cp onto stack
        // common
call:   (++sp)->aval = fp;         // push old frame pointer
        (++sp)->aval = addr;       // push new method pointer
        fp = sp-2;                 // update new frame pointer
        numParams = addr[0];       // update new num params
        numLocals = addr[1];       // update new num locals
        pp = fp-numParams;         // update param 0 pointer
        lp = fp+3;                 // update local 0 pointer
        sp += numLocals;           // make space for locals
        cp = addr+2;               // advance code pointer to first opcode
//printf("-> %s\n", curMethod(vm, fp));
        EndInstr;                  // start execution

      Case CallVirtual:
        // vtable dereference
        numParams = *(cp+3);           // num params in scode itself
        addr = (sp-numParams+1)->aval; // address of this pointer
        u2 = *(uint16_t*)addr;         // vtable block index (always first field)
        addr = block2addr(cb, u2);     // vtable block index to address pointer
#ifdef IS_BIG_ENDIAN        
        u2 = *(cp+1);                  // get method index (unaligned, big endian)
        u2 = (u2 << 8) | *(cp+2);
#else        
        u2 = *(cp+2);                  // get method index (unaligned, little endian)
        u2 = (u2 << 8) | *(cp+1);
#endif        
        u2 = ((uint16_t*)addr)[u2];    // lookup method's block index
        addr = block2addr(cb, u2);     // address of target method
        (++sp)->aval = cp+4;           // push return cp onto stack
        goto call;                     // reuse common call implementation

      Case CallNative:
#ifdef SCODE_DEBUG
        if (!isNativeIdValid(*(cp+1), *(cp+2)))
        {
          printf("ERROR: missing  native method %d::%d\n", *(cp+1), *(cp+2));
          return ERR_MISSING_NATIVE;
        }
#endif
        native = nativeTable[*(cp+1)][*(cp+2)];  // lookup native func ptr
        u2 = *(cp+3);                  // num params in scode itself
        vm->sp = sp;                   // save stack pointer before calling out
        cell = native(vm, sp-u2+1);    // call native method
        cp += 4;                       // advance to next instruction
        sp -= u2-1;                    // pop stack back down to param0
        *sp = cell;                    // push result on stack
        EndInstr;                      // keep chugging

// TODO - collapse code with CallNative?
      Case CallNativeWide:  
#ifdef SCODE_DEBUG
        if (!isNativeIdValid( *(cp+1), *(cp+2)))
        {
          printf("ERROR: missing  native method %d::%d\n", *(cp+1), *(cp+2));
          return ERR_MISSING_NATIVE;
        }
#endif
        native = nativeTable[*(cp+1)][*(cp+2)];  // lookup native func ptr
        u2 = *(cp+3);                  // num params in scode itself
        vm->sp = sp;                   // save stack pointer before calling out
        s8 = ((NativeMethodWide)native)(vm, sp-u2+1);    // call native method
        cp += 4;                       // advance to next instruction
        sp -= u2-1;                    // pop stack back down to param0
        *(int64_t*)sp = s8;            // push result on stack
        ++sp;
        EndInstr;                      // keep chugging

      Case CallNativeVoid:
        native = nativeTable[*(cp+1)][*(cp+2)];  // lookup native func ptr
        u2 = *(cp+3);                  // num params in scode itself
        vm->sp = sp;                   // save stack pointer before calling out
        native(vm, sp-u2+1);           // call native method
        cp += 4;                       // advance to next instruction
        sp -= u2;                      // pop stack back down to param0-1
        EndInstr;                      // keep chugging

      Case ReturnPop:
//printf("<- %s\n", curMethod(vm, fp));
        // check stack balancing on unwind
        #ifdef SCODE_DEBUG
          if (sp-numLocals != lp)
          {
            printf("WARNING: stack imbalance in %s\n", curMethod(vm, fp));
            printf("  lp        = %p\n", lp);
            printf("  sp        = %p\n", sp);
            printf("  sp-locals = %p\n", sp-numLocals);
            dumpStack(vm, sp);            
          }
        #endif
        cell = *sp;                // save result
        sp = fp-numParams;         // pop stack back down to param0
        cp = fp[0].aval;           // pop code pointer
        if (cp == 0)               // if unwinding main method itself
        {
          return cell.ival;
        }
        fp   = fp[1].aval;         // pop old frame pointer
        addr = fp[2].aval;         // pop old method pointer
        numParams = addr[0];       // update old num params
        numLocals = addr[1];       // update old num locals
        pp = fp-numParams;         // update param 0 pointer
        lp = fp+3;                 // update local 0 pointer
        *sp = cell;                // push result onto stack
        EndInstr;

// TODO - collapse this code with ReturnPop
      Case ReturnPopWide:
//printf("<- %s\n", curMethod(vm, fp));
        // check stack balancing on unwind
        #ifdef SCODE_DEBUG
          if (sp-1-numLocals != lp)
          {
            printf("WARNING: stack imbalance in %s\n", curMethod(vm, fp));
            printf("  lp        = %p\n", lp);
            printf("  sp        = %p\n", sp);
            printf("  sp-locals = %p\n", sp-numLocals);
            dumpStack(vm, sp);                 
          }
        #endif
        s8 = *(int64_t*)(sp-1);     // save result
        sp = fp-numParams;         // pop stack back down to param0
        cp = fp[0].aval;           // pop code pointer
        if (cp == 0)               // if unwinding main method itself
        {
          return 0;
        }
        fp   = fp[1].aval;         // pop old frame pointer
        addr = fp[2].aval;         // pop old method pointer
        numParams = addr[0];       // update old num params
        numLocals = addr[1];       // update old num locals
        pp = fp-numParams;         // update param 0 pointer
        lp = fp+3;                 // update local 0 pointer
        *(int64_t*)sp = s8;        // push result onto stack
        sp++;
        EndInstr;

      Case ReturnVoid:
//printf("<- %s\n", curMethod(vm, fp));
        // check stack balancing on unwind
        #ifdef SCODE_DEBUG
          if (sp-numLocals+1 != lp)
          {
            printf("WARNING: stack imbalance in %s\n", curMethod(vm, fp));
            printf("  lp        = %p\n", lp);
            printf("  sp        = %p\n", sp);
            printf("  sp-locals = %p\n", sp-numLocals+1);
            dumpStack(vm, sp);
          }
        #endif
        sp = fp-numParams-1;       // pop stack back down to param0-1
        cp = fp[0].aval;           // pop code pointer
        if (cp == 0)               // if unwinding main method itself
        {
          return sp->ival;
        }
        fp   = fp[1].aval;         // pop old frame pointer
        addr = fp[2].aval;         // pop old method pointer
        numParams = addr[0];       // update old num params
        numLocals = addr[1];       // update old num locals
        pp = fp-numParams;         // update param 0 pointer
        lp = fp+3;                 // update local 0 pointer
        EndInstr;

      ////////////////////////////////////////////////////////////////////
      // misc
      ////////////////////////////////////////////////////////////////////

      Case InitArray:
        initArray((sp-2)->aval, (sp-1)->ival, sp->ival);
        sp -= 3;
        ++cp;
        EndInstr;

      Case InitVirt:
        *((uint16_t*)sp->aval) = *(uint16_t*)(cp+1);
        --sp;
        cp += 3;
        EndInstr;

      Case InitComp:
        *(((uint16_t*)sp->aval)+1) = *(uint16_t*)(cp+1);
        --sp;
        cp += 3;
        EndInstr;

      Case Assert:
        if ((sp--)->ival == 0)
        {
          u2 = *(uint16_t*)(cp+1);   // get line num
          vm->assertFailures++;
          if (vm->onAssertFailure != NULL)
          {
#ifdef SCODE_DEBUG
            vm->onAssertFailure(curMethod(vm, fp), u2);
#else
            vm->onAssertFailure("?", u2);
#endif
          }
        }
        else
        {
          vm->assertSuccesses++;
        }
        cp += 3;
        EndInstr;

      Case Switch:
        // u1 Switch
        // u2 num
        // u2 jump0
        // ...
        // u2 jumpN
        u2 = *(uint16_t*)(cp+1);   // get number of items in jump table
       if (sp->ival >= u2 || sp->ival < 0)
       {
         // if cond not bounded by 0-num, then jump over
         cp += 3 + u2*2;
       }
       else
       {
         // get jump offset from jump table
         cp += *(int16_t*)(cp+ 3 + sp->ival*2);
       }
       --sp;
       EndInstr;

      Case MetaSlot:
        // ignore including block index arg
        cp += 3;
        EndInstr;


      /****************************************/
      /*  Unaccepted opcodes                  */
      /****************************************/

      // Currently never appears in scode; replaced by LoadBuf
      Case LoadArrayLiteral:

      // This should never be in scode, for Java bytecode only
      Case Cast:

      // These should never be in scode - they are just for IR
      Case SizeOf:
      Case LoadDefine:
        return ERR_UNKNOWN_OPCODE;


#ifndef COMPUTED_GOTO
      default:
        #ifdef SCODE_DEBUG
          printf("Unknown opcode %s\n", opcodeToName(*cp));
        #endif
        return ERR_UNKNOWN_OPCODE;
    }
  }
#endif
}
