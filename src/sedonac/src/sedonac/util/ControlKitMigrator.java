//
// Copyright (c) 2012 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 Jan 12  Elizabeth McKenney    Creation
//

package sedonac.util;

import java.io.*;
import java.util.*;



/**
 * ControlKitMigrator converts a Sedona app .sax file from the old open
 * source 'control' kit to the new reduced-size, more targeted kits.
 */

public class ControlKitMigrator
{

  public static void main(String[] args)
    throws Exception
  {
    if (args.length < 1)
    {
      System.out.println("usage: ControlKitMigrator <saxfile> ");
      return;
    }

    String inFname = args[0];

    if (!inFname.endsWith(".sax"))
    {
      System.out.println("Input filename must have '.sax' extension.");
      return;
    }

    // Create lookup map of comps to kits
    setupKitMap();


    // Open original sax file
    File inFile = new File(inFname);
    Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(inFile)));
    StreamTokenizer st = new StreamTokenizer(r);
    st.quoteChar(0x22);   // "
    st.quoteChar(0x27);   // '

    // Scan file, looking for control kit in schema and control components in app;
    // keep track of which new kits need to be added to schema
    int tt = st.nextToken();
    while (tt!=StreamTokenizer.TT_EOF)
    {
      if (st.sval!=null)
      {
        int dcol = st.sval.indexOf("::");
        if (dcol>=0)
        {
          String kitname  = st.sval.substring(0, dcol);
          String compname = st.sval.substring(dcol+2);
          if (kitname.equals("control"))
          {
            String needkit = (String)(compToKitMap.get((Object)compname));
            System.out.println("  Found control component: " + compname + " -> " + needkit); 
            if (!kitsReqd.contains((Object)needkit))
              kitsReqd.add(needkit);
          }
        }
      }

      tt = st.nextToken();
    }

    // Summarize findings
    System.out.println(" Kits required for new schema: " + kitsReqd.toString());


    // Open new output sax file 
    String mFilename = inFname.substring(0, inFname.lastIndexOf(".")) + "_migrated.sax";
    PrintStream pout = new PrintStream(new FileOutputStream(new File(mFilename)));

    System.out.println("Writing migrated app file to " + mFilename);


    // Reset input stream for second pass
    BufferedReader r2 = new BufferedReader(new InputStreamReader(new FileInputStream(inFile)));

    // Scan the input again, this time copying input to output with modifications:
    //   (a) subst control kit in schema with new kits as needed
    //   (b) subst kit name in control components with appropriate new kit
    boolean bSchemaDone = false;

    String nextline = r2.readLine();
    while (nextline!=null)
    {
      if (!bSchemaDone)
      {
        if (nextline.indexOf("control")>=0)
        {
          if (nextline.indexOf("'control'")>=0) 
            nextline = substKits("'");
          else if (nextline.indexOf("\"control\"")>=0)
            nextline = substKits("\"");
        } 

        bSchemaDone = (nextline.indexOf("/schema") >= 0);
      }
      else
      {
        int dcont = nextline.indexOf("control::");
        if (dcont>=0)
        {
          char quo = nextline.charAt(dcont-1);   // works; no whitespace betw kitname & quotes

          int dcol = nextline.indexOf("::");
          int equo = nextline.indexOf(quo, dcont);
          String cname = nextline.substring(dcol+2, equo);

          String newkit = (String)(compToKitMap.get((Object)cname));
          if (newkit==null)
          {
            System.out.println("ERROR!  No new kit found for comp name '" + cname + "'");
            return;
          }

          // Reassemble line with new kit name
          nextline = nextline.substring(0, dcont) + newkit + nextline.substring(dcol);
        }
      }

      // Print line to output 
      pout.print(nextline + "\n");    // use \n to avoid line end issues

      // Get next line from input
      nextline = r2.readLine();
    }

    pout.close();

  }




  //
  // substKits
  //
  private static String substKits(String q)
  {
    String substKits = "";
    for (int nk=0; nk<kitsReqd.size(); nk++)
      substKits += "  <kit name=" + q + kitsReqd.get(nk) + q + " />\n";

    return substKits;
  }


  //
  // Create lookup map of comps to kits
  //
  static void setupKitMap()
  {
    for (int k=0; k<newkits.length; k++)
      for (int c=0; c<compmap[k].length; c++)
        compToKitMap.put( compmap[k][c], newkits[k] );

    //System.out.println(" Initialized kit/comp map with " + compToKitMap.size() + " elements.");
  }


  //
  // Kit <-> Comp mapping
  //
  static HashMap compToKitMap = new HashMap(80);

  static String[]   newkits = { "func", "hvac", "logic", "math", "timing", "types" };
  static String[][] compmap = 
  {
    { "Cmpr", "Count", "Freq", "Hysteresis", "IRamp", "Limiter", "Linearize", "LP", "Ramp", 
      "SRLatch", "TickTock", "UpDn" },                      // func
    { "LSeq", "ReheatSeq", "Reset", "Tstat" },              // hvac
    { "ADemux2", "And2", "And4", "ASW4", "ASW", "B2P", "BSW", "DemuxI2B4", "ISW", "Not", 
      "Or2", "Or4", "Xor" },                                // logic
    { "Add2", "Add4", "Avg10", "AvgN", "Div2", "FloatOffset", "Max", "Min", "MinMax", "Mul2", 
      "Mul4", "Neg", "Round", "Sub2", "Sub4", "TimeAvg" },  // math
    { "DlyOff", "DlyOn", "OneShot", "Timer" },              // timing
    { "B2F", "ConstBool", "ConstFloat", "ConstInt", "F2B", "F2I", "I2F", "L2F", "WriteBool", 
      "WriteFloat", "WriteInt" }                            // types
  }; 


  //
  // List of new kits needed for migrated app
  //

  static ArrayList kitsReqd = new ArrayList();

}

