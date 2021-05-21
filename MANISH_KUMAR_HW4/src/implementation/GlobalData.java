/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import baseclasses.InstructionBase;
import baseclasses.PropertiesContainer;
import java.util.Map;
import java.util.Set;
import utilitytypes.IGlobals;
import tools.InstructionSequence;
import utilitytypes.IProperties;
import utilitytypes.IRegFile;
import utilitytypes.RegisterFile;

/**
 * As a design choice, some data elements that are accessed by multiple
 * pipeline stages are stored in a common object.
 * 
 * @author 
 */
public class GlobalData extends PropertiesContainer implements IGlobals {
    protected InstructionSequence program;
    

    @Override
    public void reset() {
        setup();
    }
    
    public static final int BRANCH_STATE_NULL = 0;
    public static final int BRANCH_STATE_WAITING = 1;
    public static final int BRANCH_STATE_TAKEN = 2;
    public static final int BRANCH_STATE_NOT_TAKEN = 3;


    @Override
    public void setup() {
        this.setProperty(PROGRAM_COUNTER, (int)0);
        this.setProperty(MAIN_MEMORY, new int[1024]);
        this.setProperty("running", false);
        
        this.setProperty("program_counter_takenbranch", (int)0);
        this.setProperty("branch_state_fetch", BRANCH_STATE_NULL);
        this.setProperty("branch_state_decode", BRANCH_STATE_NULL);
        
        
        int[] rat = new int[32];
        IRegFile register = new RegisterFile(256);
        this.setProperty("RATING", rat);
        this.setProperty("reg_file", register);
        
        
	
	
    }

    /**
     * Fetches the specified property by name, returning it as a RegisterFile.
     * Throws exception on type mismatch.
     * 
     * @param name
     * @return value
     */
    @Override
    public IRegFile getPropertyRegisterFile(String name) 
    {
        while (properties == null) return null;
        
        Object q = properties.get(name);
        while (q == null) return null;
        {
        
        if (q instanceof RegisterFile)
        {
            return (RegisterFile)q;
        } 
        else 
        {
            throw new java.lang.ClassCastException("Property " + name + 
                    " not be changed from " +
                    q.getClass().getName() + " to Reg_File.");
        }
        }
    }
    
    
    @Override
    public IRegFile getRegisterFile() 
    {
        return getPropertyRegisterFile("reg_file");
    }
    @Override
    public void loadProgram(InstructionSequence seq)
    {
        program = seq;
    }
    
    public GlobalData()
    {
        setup();
    }
    @Override
    public InstructionBase getInstructionAt(int pc_address) 
    {
        return program.getInstructionAt(pc_address);
    }

   
    
    public void advanceClock() 
    {
        super.advanceClock();
        getRegisterFile().advanceClock();
    }
}
