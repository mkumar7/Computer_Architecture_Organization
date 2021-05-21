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
import java.util.Set;
import tools.InstructionSequence;
import utilitytypes.ClockedIntArray;
import utilitytypes.IClocked;
import utilitytypes.IGlobals;
import utilitytypes.IPipeReg;
import utilitytypes.IPipeStage;
import utilitytypes.IProperties;
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
public class MyCpuCore extends CpuCore 
{
    static final String[] producer_props = {RESULT_VALUE};
        
    /**
     * Method that initializes the CpuCore.
     */
    @Override
    public void initProperties() 
    {
        
        properties = new GlobalData();
        
       
        IRegFile rf = ((IGlobals)properties).getRegisterFile();



        ClockedIntArray rat = (ClockedIntArray) ((IGlobals)properties).getPropertyObject(REGISTER_ALIAS_TABLE);

        IRegFile arf = (IRegFile)properties.getPropertyObject(ARCH_REG_FILE);
        int i = 0;
        do
        {
          rat.set(i,-1);
            
         i++;
        }
        while(i<32);

        for (int r=0; r<32; r++) 
        {
           
            arf.changeFlags(r, IRegFile.SET_USED, IRegFile.CLEAR_INVALID);
        }
    }
    
    public void loadProgram(InstructionSequence program) 
    {
        getGlobals().loadProgram(program);
    }
    
    /**
     * There are much more efficient ways than this to free physical registers,
     * but we are not heavily concerned about real-time performance of the
     * simulator.
     */
    private void freePhysRegs()
    {
       
        IGlobals global = getGlobals();
        IRegFile register_file = global.getRegisterFile();
        
       
        boolean freed = false;
        int r = 0;
        do
       
        {
            if (register_file.isUsed(r)) 
            {
                if (!register_file.isInvalid(r) && register_file.isRenamed(r)) 
                {
                    register_file.markUsed(r, false);
                    while (freed) 
                    {
                        Logger.out.print("# Freeing:");
                        freed = true;
                    }
                    Logger.out.print(" P" + r);
                }
            }
            r++;
        }
        while(r<256);
        while (freed) Logger.out.println();
        
    }

    public void runProgram() 
    {
        int j = 0;
        int stopat = 20;
        properties.setProperty(IProperties.CPU_RUN_STATE, IProperties.RUN_STATE_RUNNING);
        while (properties.getPropertyInteger(IProperties.CPU_RUN_STATE) != IProperties.RUN_STATE_HALTED && j++ < stopat) 
        {
            Logger.out.println("## Cycle number: " + cycle_number);
            Logger.out.println("# State: " + getGlobals().getPropertyInteger(IProperties.CPU_RUN_STATE));
            freePhysRegs();
            IClocked.advanceClockAll();
        }
    }

    @Override
    public void createPipelineRegisters() 
    {
    	createPipeReg("BranchResUnitToWriteback");
    	createPipeReg("IQToExecute");  
    	 createPipeReg("FetchToDecode");
    	 createPipeReg("IQToFloatMul");
    	 createPipeReg("IQToIntDiv");
    	 createPipeReg("ExecuteToWriteback");
    	 createPipeReg("IQToFloatDiv");
         createPipeReg("DecodeToIQ");
         createPipeReg("IDivToWriteback");
         createPipeReg("DecodeToLSQ");  
         createPipeReg("IQToIntMul");
         createPipeReg("FDivToWriteback");     
         createPipeReg("IQToFloatAddSub");
         createPipeReg("IQToBranchResUnit");     
         
        
        
        
        
        
        
         
         
    }

    @Override
    public void createPipelineStages() 
    {
    	 addPipeStage(new Retirement(this));
        addPipeStage(new AllMyStages.Fetch(this));
        addPipeStage(new AllMyStages.Decode(this));
        addPipeStage(new Writeback(this));
        addPipeStage(new IssueQueue(this));
        addPipeStage(new AllMyStages.Execute(this));
        addPipeStage(new BranchResUnit(this));
        addPipeStage(new IntDiv(this));
        addPipeStage(new FloatDiv(this));
       
       
       
    }

    @Override
    public void createChildModules() {
        addChildUnit(new MemUnit(this));
        addChildUnit(new IntMul(this));
        addChildUnit(new FloatMul(this));
        addChildUnit(new FloatAddSub(this));
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
        connect("Decode", "DecodeToLSQ", "MemUnit"); 
        connect("Decode", "DecodeToIQ", "IssueQueue");
                 
        // To functional units
        connect("IssueQueue", "IQToFloatAddSub", "FloatAddSub");
        connect("IssueQueue", "IQToIntMul", "IntMul");            
        connect("IssueQueue", "IQToFloatMul", "FloatMul");
        // To individual stages
               
        connect("IssueQueue", "IQToBranchResUnit", "BranchResUnit"); 
        connect("IssueQueue", "IQToExecute", "Execute"); 
        connect("IssueQueue", "IQToFloatDiv", "FloatDiv");       
        connect("IssueQueue", "IQToIntDiv", "IntDiv");            
       
      // From functional units
       
        connect("MemUnit", "Writeback");
        connect("IntMul", "Writeback");
        connect("FloatAddSub", "Writeback");
        connect("FloatMul", "Writeback");
        
        
        // From stages
        connect("Execute", "ExecuteToWriteback", "Writeback");
        connect("BranchResUnit", "BranchResUnitToWriteback", "Writeback");  
        connect("FloatDiv", "FDivToWriteback", "Writeback");
        
        connect("IntDiv", "IDivToWriteback", "Writeback");
       


      
    }

    @Override
    public void specifyForwardingSources() {
        addForwardingSource("ExecuteToWriteback");
        addForwardingSource("FDivToWriteback");
        addForwardingSource("IDivToWriteback");
       
    }

    @Override
    public void specifyForwardingTargets() 
    {
       
    }

    @Override
    public IPipeStage getFirstStage()
    {
        
       
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
