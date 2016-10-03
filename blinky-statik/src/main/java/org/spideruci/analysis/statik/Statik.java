package org.spideruci.analysis.statik;

import static org.spideruci.analysis.statik.SootCommander.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.spideruci.analysis.statik.controlflow.Graph;
import org.spideruci.analysis.statik.flow.StatikFlowGraph;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

/**
 * @author vpalepu
 */
public class Statik {
  
  public static final String STARTUP_MSG = 
      "Starting ``Blinky Statik``, a static analysis framework for JVM executables."
      + "\nThis framework currently uses algorithms from Soot and Heros to"
      + "\nperform static analysis.\n";
  
  public static final String CP_SEP = System.getProperty("path.separator");
  public static final String FP_SEP = System.getProperty("file.separator");
  
  public static final String RTJAR = "/rt.jar";
  public static final String JCEJAR = "/jce.jar";
  
  private static AnalysisConfig startup(final String[] args) {
    System.out.println(STARTUP_MSG);
    
    final String configPath = args[0];
    AnalysisConfig analysisconfig = AnalysisConfig.init(configPath);
    final String jre7path = analysisconfig.get(AnalysisConfig.JRE7_LIB);
    
    System.out.println(jre7path);
    
    List<String> argsList = new ArrayList<>();
    argsList.addAll(Arrays.asList(new String[] {
        "--keep-line-number",
        "-cp",
        jre7path + "/rt.jar:" + jre7path + "/jce.jar:" + "./target/test-classes/",
//        ".:" + jre7path + "/rt.jar:" + jre7path + "/jce.jar:" + "/Users/Ku/Documents/uci/research/joda-time/target/joda-time-2.9.4-jar-with-dependencies.jar:" + "/Users/Ku/Documents/uci/research/blinky/blinky-statik/target/test-classes",
//        jre7path + "/rt.jar:" + jre7path + "/jce.jar:" + "/Users/Ku/Documents/uci/research/joda-time/target/classes/org/joda/time/:" + "target/test-classes/" ,
        "-w",
        analysisconfig.get(AnalysisConfig.ARG_CLASS)
    }));
    
    analysisconfig.setArgs(argsList);
    return analysisconfig;
  }
  
  public static void main(String[] args) {
    AnalysisConfig analysisconfig = startup(args);
    DummyMainManager.setupDummyMain();
    
    StatikCallGraphBuilder cgBuilder =  StatikCallGraphBuilder.create("call-graph");
    cgBuilder.addEntryPoint(
        analysisconfig.get(AnalysisConfig.ENTRY_CLASS), 
        analysisconfig.get(AnalysisConfig.ENTRY_METHOD));
    //
    SootMethod main = Scene.v().getMainClass().getMethodByName("main");
//    SootMethod main = Scene.v().getMainClass().getMethodByName("testConstants");
//    System.out.println(Scene.v().get);
    //
    cgBuilder.hookupWithSoot();
    
    RUN_SOOT(analysisconfig.getArgs());
    cgBuilder.buildCallGraph();

    CallGraph cg = cgBuilder.getCallGraph();
    StatikFlowGraph flowGraph = StatikFlowGraph.init(cg, GET_ENTRY_METHODS());
    //
//    flowGraph.addEntryPoint(main);
    
    ArrayList<SootMethod> visitedNodes = flowGraph.visitMethodsTopDown();

    System.out.println("---- Call Graph ----");
    for(SootMethod visitedNode : visitedNodes) {
      SootMethod m = visitedNode;
      System.out.println(m);
      
      if(!m.isConcrete())
        continue;
      
      Iterator<Edge> edges = cg.edgesOutOf(m);
      
      while(edges.hasNext()) {
        Edge edge = edges.next();
        
        Unit sourceUnit = edge.srcUnit();
        SootMethod targetMethod = edge.tgt();
        System.out.format("\t%s, calls: %s\n", sourceUnit, targetMethod);
      }
    }
    
    System.out.println("---- I C F G ----");
    
    flowGraph.buildIcfg();
    Graph<Unit> icfg = flowGraph.getIcfg();
    System.out.println(icfg);
    
    System.out.println("---- Java Source Icfg ----");
    System.out.println(flowGraph.icfgMgr.icfgJavaSourceLines());
  }

}
