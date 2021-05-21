/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import tools.MyALU;
import utilitytypes.EnumOpcode;
import baseclasses.InstructionBase;
import baseclasses.PipelineRegister;
import baseclasses.PipelineStageBase;
import voidtypes.VoidLatch;
import baseclasses.CpuCore;
import baseclasses.Latch;
import cpusimulator.CpuSimulator;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import utilitytypes.ClockedIntArray;
import static utilitytypes.EnumOpcode.*;
import utilitytypes.ICpuCore;
import utilitytypes.IGlobals;
import utilitytypes.IPipeReg;
import utilitytypes.IProperties;
import static utilitytypes.IProperties.*;
import utilitytypes.IRegFile;
import utilitytypes.Logger;
import utilitytypes.Operand;
import voidtypes.VoidLabelTarget;

/**
 * The AllMyStages class merely collects together all of the pipeline stage 
 * classes into one place.  You are free to split them out into top-level
 * classes.
 * 
 * Each inner class here implements the logic for a pipeline stage.
 * 
 * It is recommended that the compute methods be idempotent.  This means
 * that if compute is called multiple times in a clock cycle, it should
 * compute the same output for the same input.
 * 
 * How might we make updating the program counter idempotent?
 * 
 * @author
 */
public class AllMyStages {
    /*** Fetch Stage ***/
    static class Fetch extends PipelineStageBase {
        public Fetch(ICpuCore core) {
            super(core, "Fetch");
        }
        
        // Does this state have an instruction it wants to send to the next
        // stage?  Note that this is computed only for display and debugging
        // purposes.
        boolean has_work;
                
        /** 
         * For Fetch, this method only has diagnostic value.  However, 
         * stageHasWorkToDo is very important for other stages.
         * 
         * @return Status of Fetch, indicating that it has fetched an 
         *         instruction that needs to be sent to Decode.
         */
        @Override
        public boolean stageHasWorkToDo() {
            return has_work;
        }
        
        @Override
        public String getStatus() {
            IGlobals globals = (GlobalData)getCore().getGlobals();
            if (globals.getPropertyInteger("branch_state_fetch") == GlobalData.BRANCH_STATE_WAITING) {
                addStatusWord("ResolveWait");
            }
            return super.getStatus();
        }

        @Override
        public void compute(Latch input, Latch output) {
            IGlobals globals = (GlobalData)getCore().getGlobals();
            
            // Get the PC and fetch the instruction
            int pc_no_branch    = globals.getPropertyInteger(PROGRAM_COUNTER);
            int pc_taken_branch = globals.getPropertyInteger("program_counter_takenbranch");
            int branch_state_decode = globals.getPropertyInteger("branch_state_decode");
            int branch_state_fetch = globals.getPropertyInteger("branch_state_fetch");
            int pc = (branch_state_decode == GlobalData.BRANCH_STATE_TAKEN) ?
                    pc_taken_branch : pc_no_branch;
            InstructionBase ins = globals.getInstructionAt(pc);
            
            // Initialize this status flag to assume a stall or bubble condition
            // by default.
            has_work = false;
            
            // If the instruction is NULL (like we ran off the end of the
            // program), just return.  However, for diagnostic purposes,
            // we make sure something meaningful appears when 
            // CpuSimulator.printStagesEveryCycle is set to true.
            if (ins.isNull()) {
                // Fetch is working on no instruction at no address
                setActivity("");
            } else {            
                // Since there is no input pipeline register, we have to inform
                // the diagnostic helper code explicitly what instruction Fetch
                // is working on.
                has_work = true;
                output.setInstruction(ins);
                setActivity(ins.toString());
            }
            
            // If the output cannot accept work, then 
            if (!output.canAcceptWork()) return;
            
            globals.setClockedProperty(PROGRAM_COUNTER, pc + 1);
            
            boolean branch_wait = false;
            if (branch_state_fetch == GlobalData.BRANCH_STATE_WAITING) {
                branch_wait = true;
            }
            if (branch_state_decode != GlobalData.BRANCH_STATE_NULL) {
                globals.setClockedProperty("branch_state_fetch", GlobalData.BRANCH_STATE_NULL);
                globals.setClockedProperty("branch_state_decode", GlobalData.BRANCH_STATE_NULL);
                branch_wait = false;
            }
            if (!branch_wait) {
                if (ins.getOpcode().isBranch()) {
                    globals.setClockedProperty("branch_state_fetch", GlobalData.BRANCH_STATE_WAITING);
                }
            }
        }
    }

    
    /*** Decode Stage ***/
    static class Decode extends PipelineStageBase {
        public Decode(ICpuCore core) {
            super(core, "Decode");
        }
        
        
        // When a branch is taken, we have to squash the next instruction
        // sent in by Fetch, because it is the fall-through that we don't
        // want to execute.  This flag is set only for status reporting purposes.
        boolean squashing_instruction = false;
        boolean shutting_down = false;

        @Override
        public String getStatus() {
            IGlobals globals = (GlobalData)getCore().getGlobals();
            String s = super.getStatus();
            if (globals.getPropertyBoolean("decode_squash")) {
                s = "Squashing";
            }
            return s;
        }
        
        
        
        static final EnumSet<EnumOpcode> floatAddSubSet = 
                EnumSet.of(FCMP,FSUB,FADD);

        private void renameDestReg(int new_pregister, Operand op, IGlobals globals) 
        {
        	
           


            IRegFile register_file = globals.getRegisterFile();

            ClockedIntArray rating = (ClockedIntArray) globals.getPropertyObject(REGISTER_ALIAS_TABLE);
            int ar_register = op.getRegisterNumber();
            int old_preg = rating.get(ar_register);
            IRegFile arf = (IRegFile)globals.getPropertyObject(ARCH_REG_FILE);
            

            if(old_preg == -1)
            {
                Logger.out.println("Destination R" + ar_register + " R" + ar_register + " unlinked, P" + new_pregister + " allocation");
            }
            else 
            {

                Logger.out.println("Destination R" + ar_register + " P" + old_preg + " release, P" + new_pregister + " allocation");
            }
            register_file.markNewlyAllocated(new_pregister);
            rating.set(ar_register,new_pregister);




            op.rename(new_pregister);
        }
        
        @Override
        public void compute() 
        {
            
        	while (shutting_down) 
            {
                addStatusWord("Shut down");
                setActivity("");
                return;
            }
            Latch input = readInput(0);
            input = input.duplicate();
            InstructionBase ins = input.getInstruction();

            // Default to no squashing.
            squashing_instruction = false;
            
            setActivity(ins.toString());

            IGlobals globals = (GlobalData)getCore().getGlobals();
            if (globals.getPropertyBoolean("decode_squash")) {
                // Drop the fall-through instruction.
                globals.setClockedProperty("decode_squash", false);
                squashing_instruction = false;
                
               
                // Since we don't pass an instruction to the next stage,
                // must explicitly call input.consume in the case that
                // the next stage is busy.
                input.consume();
                return;
            }
            
            while (ins.isNull()) return;
            Operand source1  = ins.getSrc1();
            Operand source2  = ins.getSrc2();
            Operand operand0 = ins.getOper0();
            EnumOpcode opcode = ins.getOpcode();
           
           
            IRegFile register_file = globals.getRegisterFile();

            ClockedIntArray rating = (ClockedIntArray) globals.getPropertyObject(REGISTER_ALIAS_TABLE);
            
           
           
            if (source1.isRegister()) 
            {
                source1.rename(rating.get(source1.getRegisterNumber()));
            }
            if (source2.isRegister()) 
            {
                source2.rename(rating.get(source2.getRegisterNumber()));
            }
            if (opcode.oper0IsSource() && operand0.isRegister()) 
            {
                operand0.rename(rating.get(operand0.getRegisterNumber()));
            }
           
            int target_register = -1;
            if (opcode.needsWriteback()) 
            {
                int oper0reg = operand0.getRegisterNumber();
                int i = 0;
                do
                	
                			
                //for (int p=0; p<256; p++) 
                {
                    if (!register_file.isUsed(i)) 
                    {
                        target_register = i;
                        break;
                    }
                    i++;
                }
                while(i<256);
                while (target_register < 0) 
                {
                    setResourceWait("physical registers not free");
                    return;
                }
            }
            
            // See what operands can be fetched from the register file
            registerFileLookup(input);
            
            // See what operands can be fetched by forwarding
            forwardingSearch(input);
            int opt_number;
            Latch output;
            
            
            boolean take_branch = false;
            int val0 = 0;
            int val1 = 0;
            if(opcode.accessesMemory())
            {
                opt_number = lookupOutput("DecodeToLSQ");
                output = this.newOutput(opt_number);
            }

            else
            {
                opt_number = lookupOutput("DecodeToIQ");
                output = this.newOutput(opt_number);
            }
           
           

            switch (opcode) {
                case BRA:
                    if (!operand0.hasValue()) {
                        // If we do not already have a value for the branch
                        // condition register, must stall.
                        this.setResourceWait(operand0.getRegisterName());
                       
                        return;
                    }
                    val0 = operand0.getValue();
                    
                    // The CMP instruction just sets its destination to
                    // (src1-src2).  The result of that is in oper0 for the
                    // BRA instruction.  See comment in MyALU.java.
                    switch (ins.getComparison()) {
                        case EQ:
                            take_branch = (val0 == 0);
                            break;
                        case NE:
                            take_branch = (val0 != 0);
                            break;
                        case GT:
                            take_branch = (val0 > 0);
                            break;
                        case GE:
                            take_branch = (val0 >= 0);
                            break;
                        case LT:
                            take_branch = (val0 < 0);
                            break;
                        case LE:
                            take_branch = (val0 <= 0);
                            break;
                    }
                    
                    if (take_branch) {
                        // If the branch is taken, send a signal to Fetch
                        // that specifies the branch target address, via
                        // "globals.next_program_counter_takenbranch".  
                        // If the label is valid, then use its address.  
                        // Otherwise, the target address will be found in 
                        // src1.
                        if (ins.getLabelTarget().isNull()) {
                            // If branching to address in register, make sure
                            // operand is valid.
                            if (!source1.hasValue()) {
                                this.setResourceWait(source1.getRegisterName());
                                
                                return;
                            }
                            
                            val1 = source1.getValue();
                        } else {
                            val1 = ins.getLabelTarget().getAddress();
                        }
                        globals.setClockedProperty("program_counter_takenbranch", val1);
                        
                        // Send a signal to Fetch, indicating that the branch
                        // is resolved taken.  This will be picked up by
                        // Fetch.advanceClock on the same clock cycle.
                        globals.setClockedProperty("branch_state_decode", GlobalData.BRANCH_STATE_TAKEN);
                        globals.setClockedProperty("decode_squash", true);
                    } else {
                        // Send a signal to Fetch, indicating that the branch
                        // is resolved not taken.
                        globals.setClockedProperty("branch_state_decode", GlobalData.BRANCH_STATE_NOT_TAKEN);
                    }
                    
                    // Since we don't pass an instruction to the next stage,
                    // must explicitly call input.consume in the case that
                    // the next stage is busy.
                    // NOTE:  For HW6, we WILL have to pass this to the IQ.
                    input.consume();
                    return;
                    
                case JMP:
                    // JMP is an inconditionally taken branch.  If the
                    // label is valid, then take its address.  Otherwise
                    // its operand0 contains the target address.
                    if (ins.getLabelTarget().isNull()) {
                        if (!operand0.hasValue()) {
                            // If branching to address in register, make sure
                            // operand is valid.
                            this.setResourceWait(operand0.getRegisterName());
                            // Nothing else to do.  Bail out.
                            return;
                        }
                        
                        val0 = operand0.getValue();
                    } else {
                        val0 = ins.getLabelTarget().getAddress();
                    }
                    globals.setClockedProperty("program_counter_takenbranch", val0);
                    globals.setClockedProperty("branch_state_decode", GlobalData.BRANCH_STATE_TAKEN);
                    globals.setClockedProperty("decode_squash", true);
                    
                    // Since we don't pass an instruction to the next stage,
                    // must explicitly call input.consume in the case that
                    // the next stage is busy.
                   
                    input.consume();
                    return;
                    
                case CALL:
                    // CALL is an inconditionally taken branch.  If the
                    // label is valid, then take its address.  Otherwise
                    // its src1 contains the target address.
                    if (ins.getLabelTarget().isNull()) {
                        if (!source1.hasValue()) {
                            // If branching to address in register, make sure
                            // operand is valid.
                            this.setResourceWait(source1.getRegisterName());
                            // Nothing else to do.  Bail out.
                            return;
                        }
                        
                        val1 = source1.getValue();
                    } else {
                        val1 = ins.getLabelTarget().getAddress();
                    }
                    
                    // CALL also has a destination register, which is oper0.
                    // Before we can resolve the branch, we have to MAKE SURE
                    // that the return address can be passed to Writeback
                    // through Execute before we go setting any globals.
                    while (!output.canAcceptWork()) return;
                    
                    // To get the return address into Writeback, we will
                    // replace the instruction's source operands with the
                    // address of the instruction and a constant 1.
                   
                    
                    Operand pc_operand = Operand.newRegister(Operand.PC_REGNUM);
                    pc_operand.setIntValue(ins.getPCAddress());
                    renameDestReg(target_register, operand0, globals);
                    ins.setSrc1(pc_operand);
                    ins.setSrc2(Operand.newLiteralSource(1));
                    ins.setLabelTarget(VoidLabelTarget.getVoidLabelTarget());
                    
                    output.setInstruction(ins);
                    
                    globals.setClockedProperty("program_counter_takenbranch", val1);
                    globals.setClockedProperty("branch_state_decode", GlobalData.BRANCH_STATE_TAKEN);
                    globals.setClockedProperty("decode_squash", true);
                    
                   
                    return;
            }

           if (!output.canAcceptWork()) return;
            if (ins.getOpcode() == EnumOpcode.HALT) shutting_down = true;            
            
           
            
            output.copyAllPropertiesFrom(input);
           
            output.setInstruction(ins);
           
            output.write();

           
            input.consume();
            if (opcode.needsWriteback()) 
            {
                renameDestReg(target_register, operand0, globals);
            }

        }
    }
    

    /*** Execute Stage ***/
    static class Execute extends PipelineStageBase {
        public Execute(ICpuCore core) {
            super(core, "Execute");
        }

        @Override
        public void compute(Latch input, Latch output) {
            if (input.isNull()) return;
            doPostedForwarding(input);
            InstructionBase ins = input.getInstruction();

            int source1 = ins.getSrc1().getValue();
            int source2 = ins.getSrc2().getValue();
            int oper0 =   ins.getOper0().getValue();

            int result = MyALU.execute(ins.getOpcode(), source1, source2, oper0);
                        
            boolean isfloat = ins.getSrc1().isFloat() || ins.getSrc2().isFloat();
            output.setResultValue(result, isfloat);
            output.setInstruction(ins);
        }
    }
    


}
