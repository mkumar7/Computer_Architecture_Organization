/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import baseclasses.ClockedPropertiesContainer;
import baseclasses.InstructionBase;
import baseclasses.PropertiesContainer;
import java.util.Map;
import java.util.Set;
import utilitytypes.IGlobals;
import tools.InstructionSequence;
import utilitytypes.ClockedIntArray;
import utilitytypes.IProperties;
import utilitytypes.IRegFile;
import utilitytypes.RegisterFile;

/**
 * As a design choice, some data elements that are accessed by multiple
 * pipeline stages are stored in a common object.
 * 
 * @author 
 */
public class GlobalData extends ClockedPropertiesContainer implements IGlobals {
    protected InstructionSequence program;
   
    public static final int BRANCH_STATE_NULL = 0;
    public static final int BRANCH_STATE_WAITING = 1;
    public static final int BRANCH_STATE_TAKEN = 2;
    public static final int BRANCH_STATE_NOT_TAKEN = 3;
    
    public static int PRF_SIZE = 256;
    public static int ROB_SIZE = 256;
    public static int ARF_SIZE = 32;
    public static int LSQ_SIZE = 32;
    public static int IQ_SIZE = 256;
    public static int RAM_SIZE = 1024;

    
  
   
    
    
    

    @Override
    public void reset() {
        setup();
    }


    @Override
    public void setup() {
        this.setProperty(PROGRAM_COUNTER, (int)0);
        this.setProperty(MAIN_MEMORY, new int[RAM_SIZE]);
        this.setProperty(CPU_RUN_STATE, RUN_STATE_NULL);
        this.setProperty(REG_BRANCH_TARGET, -1);
        this.setProperty(FETCH_BRANCH_STATE, BRANCH_STATE_NULL);
        this.setProperty(ROB_USED, 0);
        this.setProperty(ROB_HEAD, 0);
        this.setProperty(ROB_TAIL, 0);

      
        IRegFile prf = new RegisterFile(PRF_SIZE);

       

       
        IRegFile arf = new RegisterFile(ARF_SIZE);
        this.setProperty(ARCH_REG_FILE, arf);
        this.setProperty(REGISTER_FILE, prf);
        InstructionBase[] rob = new InstructionBase[ROB_SIZE];
        this.setProperty(REORDER_BUFFER, rob);

       
        this.setProperty(REGISTER_ALIAS_TABLE, new ClockedIntArray(32));
    }

    /**
     * Fetches the specified property by name, returning it as a RegisterFile.
     * Throws exception on type mismatch.
     * 
     * @param name
     * @return value
     */
    @Override
    public IRegFile getPropertyRegisterFile(String name) {
        if (properties == null) return null;
        
        Object p = properties.get(name);
        if (p == null) return null;
        
        if (p instanceof RegisterFile) {
            return (RegisterFile)p;
        } else {
            throw new java.lang.ClassCastException("Property " + name + 
                    " cannot be converted from " +
                    p.getClass().getName() + " to RegisterFile.");
        }
    }

    @Override
    public InstructionBase[] getPropertyInstructionArr(String name) {
        if (properties == null) return null;

        Object p = properties.get(name);
        if (p == null) return null;

        if (p instanceof InstructionBase[]) {
            return (InstructionBase[])p;
        } else {
            throw new java.lang.ClassCastException("Property " + name +
                    " cannot be converted from " +
                    p.getClass().getName() + " to InstructionBase[].");
        }
    }
    
    
    @Override
    public InstructionBase getInstructionAt(int pc_address) 
    {
        return program.getInstructionAt(pc_address);
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

    /**
     * Compatibility method to get the register file, although it is now
     * stored as a named property.
     * @return 
     */
    @Override
    public IRegFile getRegisterFile() {
        return getPropertyRegisterFile(REGISTER_FILE);
    }
    

}
