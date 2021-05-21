/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import baseclasses.PipelineRegister;
import baseclasses.PipelineStageBase;
import baseclasses.CpuCore;
import examples.MultiStageFunctionalUnit;
import tools.InstructionSequence;
import utilitytypes.IGlobals;
import utilitytypes.IPipeReg;
import utilitytypes.IPipeStage;
import utilitytypes.IRegFile;

import static utilitytypes.IProperties.*;
import utilitytypes.Logger;
import voidtypes.VoidRegister;

/**
 * This is an example of a class that builds a specific CPU simulator out of
 * pipeline stages and pipeline registers.
 * 
 * @author 
 */
public class MyCpuCore extends CpuCore {
    static final String[] producer_props = {RESULT_VALUE};
        
    public void initProperties()
    {
    	 int i =0;
        properties = new GlobalData();
        int[] rat = (int[])properties.getPropertyObject("RATING");
        IGlobals globals = (GlobalData)getCore().getGlobals();
        IRegFile register = globals.getPropertyRegisterFile("reg_file");
      
        register.markPhysical();
       
       
        do
        {
        	register.markUsed(i, true);
        	i++;
        }
        while(i<32);
      
        while(i<32)
        {
        	rat[i]=i;
        	i++;
        }
        
        
    }
    
    private void freeRegister()
    {
    	int i = 0;
    	IGlobals globals = (GlobalData)getCore().getGlobals();
    	int[] rat = (int[])globals.getPropertyObject("RATING");
    	
    	
    	IRegFile regfile = globals.getRegisterFile();
    	
    	
    	do
    	{
    		while(regfile.isValid(i) && regfile.isRenamed(i) && regfile.isUsed(i))
    		{
    			regfile.markUsed(i, false);
    		}
    		i++;
    	}
    	while(i<32);
    }
    
    public void loadProgram(InstructionSequence program) 
    {
        getGlobals().loadProgram(program);
        
    }
    
    public void runProgram() {
        properties.setProperty("running", true);
        while (properties.getPropertyBoolean("running")) 
        {
            Logger.out.println("## Cycle number: " + cycle_number); 
            advanceClock();
        }
    }

    @Override
    public void createPipelineRegisters() {
        // To individual stages
      
        createPipeReg("FetchToDecode");
        
        createPipeReg("DecodeToExecute");
        
        createPipeReg("DecodeToMemory");
       
        createPipeReg("ExecuteToWriteback");
        
       createPipeReg("DecodeToFloatMul");
       
       createPipeReg("DecodeToFloatAddSub");
        
        createPipeReg("DecodeToIntMul");
        
        createPipeReg("FDivToWriteback");
        
        createPipeReg("DecodeToFloatDiv");
        
        createPipeReg("IDivToWriteback");
        
        createPipeReg("DecodeToIntDiv");
    }

    @Override
    public void createPipelineStages() 
    {
        addPipeStage(new AllMyStages.Fetch(this));
        addPipeStage(new AllMyStages.Decode(this));
        addPipeStage(new AllMyStages.Execute(this));
        addPipeStage(new IntDiv(this));
        addPipeStage(new FloatDiv(this));
        addPipeStage(new AllMyStages.Writeback(this));
    }

    @Override
    public void createChildModules() 
    {
    	addChildUnit(new FloatMul(this));
        addChildUnit(new MemUnit(this));
       
       
        addChildUnit(new FloatAddSub(this));
        addChildUnit(new IntMul(this));
    }

    @Override
    public void createConnections() {
        // Connect pipeline elements by name.  Notice that 
        // Decode has two outputs, anle to send to either Memory OR Execute 
        // and that Writeback has two inputs, able to receive from both
        // Execute and Memory.  
        // Memory no longer connects to Execute.  It is now a fully 
        // independent functional unit, parallel to Execute.
        connect("Fetch", "FetchToDecode", "Decode");
                
        // To individual stages
      
        connect("Decode","DecodeToIntMul","IntMul");
        
        connect("Decode","DecodeToFloatMul","FloatMul");
        
        connect("Decode","DecodeToFloatAddSub","FloatAddSub");
        
        connect("Decode","DecodeToFloatDiv","FloatDiv");
        
        connect("Decode","DecodeToIntDiv","IntDiv");
        
        connect("Decode", "DecodeToMemory", "MemUnit");
        
        connect("Decode", "DecodeToExecute", "Execute");
        
        
         // Writeback has multiple input connections from different execute
        // units.  The output from MSFU is really called "MSFU.Delay.out",
        // which was aliased to "MSFU.out" so that it would be automatically
        // identified as an output from MSFU.
        connect("Execute","ExecuteToWriteback", "Writeback");
        
        connect("FloatAddSub", "Writeback");
        
        connect("MemUnit", "Writeback");
        
        connect("FloatMul", "Writeback");
        
        connect("IntDiv","IDivToWriteback", "Writeback");
        
        connect("IntMul", "Writeback");
        
        connect("FloatDiv","FDivToWriteback", "Writeback");
        
    }

    @Override
    public void specifyForwardingSources() 
    {
    	addForwardingSource("IDivToWriteback");
    	addForwardingSource("FDivToWriteback");
        addForwardingSource("ExecuteToWriteback");
        
        
        // Forwarding sources for submodules are specified in the
        // specifyForwardingSources method of each module.
//        addForwardingSource("IntMul.out");    // TODO:  Find the output automatically
//        addForwardingSource("FloatMul.out");
//        addForwardingSource("FloatAddSub.out");
    }

    @Override
    public void specifyForwardingTargets() 
    {
        // Not really used for anything yet
    }

    @Override
    public IPipeStage getFirstStage()
    {
        // CpuCore will sort stages into an optimal ordering.  This provides
        // the starting point.
        return getPipeStage("Fetch");
    }
    
    public MyCpuCore() 
    {
        super(null, "core");
        initModule();
        printHierarchy();
        Logger.out.println("");
    }
}
