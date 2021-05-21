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
import static utilitytypes.IProperties.*;
import utilitytypes.IRegFile;
import utilitytypes.Logger;
import voidtypes.VoidRegister;

/**
 * This is an example of a class that builds a specific CPU simulator out of
 * pipeline stages and pipeline registers.
 * 
 * @author 
 */
public class MyCpuCore extends CpuCore {
    static final String[] prod_proportion = {RESULT_VALUE}; 
        
    public void initProperties() {
        properties = new GlobalData();
        int[] rating = ((IGlobals)properties).getPropertyIntArray("rating");
        IRegFile register_file = ((IGlobals)properties).getRegisterFile();
        int i = 0;
        do 
        {
            register_file.changeFlags(i, IRegFile.SET_USED, 0);
            rating[i] = i;
            i++;
        }
        while(i<128);
    }
    
    public void loadProgram(InstructionSequence program) 
    {
        getGlobals().loadProgram(program);
    }
    
    private void Physical_Register() 
    {
        IGlobals globals = getGlobals();
        IRegFile register_file = globals.getRegisterFile();
        boolean evaluate = false;
        
       
        int j=0;
        do
        {
            if (register_file.isUsed(j))
            {
                if (!register_file.isInvalid(j) && register_file.isRenamed(j)) 
                {
                    register_file.markUsed(j, false);
                    if (!evaluate) {
                        Logger.out.print("**Freeing:**");
                        evaluate = true;
                    }
                    Logger.out.print(" P " + j);
                }
            }
            j++;
        }
        while(j<512);
        if (evaluate) Logger.out.println();
    }
    
    public void runProgram()
    {
    	int halt = 800; 
    	 int k=0;
        properties.setProperty("running", true);
        while (properties.getPropertyBoolean("running") && k++ < halt) 
        {
            Logger.out.println("## Cycle number: " + cycle_number);
            Physical_Register();
            advanceClock();
        }
    }

    @Override
    public void createPipelineRegisters() 
    {
        // To individual stages
        createPipeReg("FetchToDecode");
        createPipeReg("IQToExecute");
        createPipeReg("IQToFloatMul");
         createPipeReg("IQToIntDiv");
        createPipeReg("ExecuteToWriteback");
        createPipeReg("IQToFloatDiv");
        createPipeReg("DecodeToIQ");
        createPipeReg("IDivToWriteback");
        createPipeReg("IQToIntMul");
        createPipeReg("FDivToWriteback");
        createPipeReg("IQToMemory");
        createPipeReg("IQToFloatAddSub");
       
     }

    @Override
    public void createPipelineStages() 
    {
    	 addPipeStage(new IssueQueue(this));
    	 addPipeStage(new AllMyStages.Writeback(this));
        addPipeStage(new AllMyStages.Fetch(this));
        addPipeStage(new AllMyStages.Decode(this));
        addPipeStage(new FloatDiv(this));
        addPipeStage(new AllMyStages.Execute(this));
        addPipeStage(new IntDiv(this));
       
    }

    @Override
    public void createChildModules() 
    {
    	addChildUnit(new FloatMul(this));
        addChildUnit(new FloatAddSub(this));
        addChildUnit(new IntMul(this));
        addChildUnit(new MemUnit(this));
        
    }

    @Override
    public void createConnections()
    {
        // Connect pipeline elements by name.  Notice that 
        // Decode has two outputs, anle to send to either Memory OR Execute 
        // and that Writeback has two inputs, able to receive from both
        // Execute and Memory.  
        // Memory no longer connects to Execute.  It is now a fully 
        // independent functional unit, parallel to Execute.
        connect("Fetch", "FetchToDecode", "Decode");
        connect("Decode", "DecodeToIQ", "IssueQueue");
        connect("IssueQueue", "IQToMemory", "MemUnit");        
        connect("IssueQueue", "IQToIntMul", "IntMul");    
        connect("IssueQueue", "IQToExecute", "Execute"); 
        connect("FloatAddSub", "Writeback");
        connect("IssueQueue", "IQToFloatDiv", "FloatDiv");     
        connect("IssueQueue", "IQToIntDiv", "IntDiv");       
        connect("IssueQueue", "IQToFloatAddSub", "FloatAddSub");
        connect("MemUnit", "Writeback");
        connect("Execute", "ExecuteToWriteback", "Writeback");
        connect("FloatDiv", "FDivToWriteback", "Writeback");
        connect("FloatMul", "Writeback");
        connect("IntMul", "Writeback");
        connect("IntDiv", "IDivToWriteback", "Writeback");
        connect("IssueQueue", "IQToFloatMul", "FloatMul");        
        
        
    }

    @Override
    public void specifyForwardingSources() 
    {
    	 addForwardingSource("IDivToWriteback");
        addForwardingSource("ExecuteToWriteback");
        addForwardingSource("FDivToWriteback");
       
    
    }

    @Override
    public void specifyForwardingTargets() {
        // Not really used for anything yet
    }

    @Override
    public IPipeStage getFirstStage() {
        // CpuCore will sort stages into an optimal ordering.  This provides
        // the starting point.
        return getPipeStage("Fetch");
    }
    
    public MyCpuCore() {
        super(null, "core");
        initModule();
        printHierarchy();
        Logger.out.println("");
    }
}
