
package implementation;

import implementation.AllMyLatches.*;
import utilitytypes.EnumOpcode;
import baseclasses.InstructionBase;
import baseclasses.PipelineRegister;
import baseclasses.PipelineStageBase;
import voidtypes.VoidLatch;
import baseclasses.CpuCore;
import cpusimulator.CpuSimulator;

import static utilitytypes.EnumOpcode.BRA;
import static utilitytypes.EnumOpcode.JMP;
import static utilitytypes.EnumOpcode.CALL;


import voidtypes.VoidInstruction;
import utilitytypes.Operand;

import java.util.List;
import java.util.ArrayList;


public class AllMyStages
{
    // Fetch Stage
    public static class Fetch extends PipelineStageBase<VoidLatch,FetchToDecode>
    {
        public Fetch(CpuCore core, PipelineRegister input, PipelineRegister output)
        {
            super(core, input, output);
        }


        boolean done;



        public boolean stageHasWorkToDo()
        {
            return done;
        }



        public String getStatus()
        {
            String m= super.getStatus();
            GlobalData globals = (GlobalData)core.getGlobalResources();


            if (globals.cur_br == GlobalData.EnumBranchState.WAIT) {

                if (m.length() > 0) m +=  ", ";
                m += "resolveWait";
            }
            return m;
        }




        @Override
        public void compute(VoidLatch input, FetchToDecode output)
        {


            GlobalData globals = (GlobalData)core.getGlobalResources();


            int PC = globals.prgcount;
            InstructionBase ins = globals.program.getInstructionAt(PC);



            done = false;



            while (globals.cur_br != GlobalData.EnumBranchState.NONE)
            {

                setActivity("BRA-BUB");

                return;
            }



            while (ins.isNull())
            {

                setActivity("NONE");

                return;
            }




            globals.next_prgcount_nobranch = PC + 1;


            done = true;
            setActivity(ins.toString());




            if (ins.getOpcode().isBranch())
            {
                globals.nxt_br = GlobalData.EnumBranchState.WAIT;
            }


            output.setInstruction(ins);
        }



        @Override
        public void advanceClock()
        {

            if (nextStageCanAcceptWork())
            {
                GlobalData globals = (GlobalData)core.getGlobalResources();


                if (globals.cur_br == GlobalData.EnumBranchState.WAIT)
                {
                    while (globals.nxt_br_dec != GlobalData.EnumBranchState.NONE)
                    {

                        switch (globals.nxt_br_dec)
                        {


                            case NO:

                                globals.prgcount = globals.next_prgcount_nobranch;
                                break;

                            case YES:
                                globals.prgcount = globals.next_prgcount_YESbranch;
                                break;

                        }

                        globals.nxt_br_dec = GlobalData.EnumBranchState.NONE;

                        globals.cur_br = GlobalData.EnumBranchState.NONE;
                    }

                }
                else
                {
                    if(globals.nxt_br != GlobalData.EnumBranchState.NONE)
                    {

                        globals.cur_br = globals.nxt_br;


                        globals.nxt_br = GlobalData.EnumBranchState.NONE;
                    }
                    else
                    {
                        globals.prgcount = globals.next_prgcount_nobranch;
                    }
                }
            }
        }
    }


    // Decode Stage
    public static class Decode extends PipelineStageBase<FetchToDecode,DecodeToExecute>
    {
        public Decode(CpuCore core, PipelineRegister input, PipelineRegister output)
        {
            super(core, input, output);
        }




        boolean wrong_reg_stall = false;

        @Override
        public boolean stageWaitingOnResource()
        {
            return wrong_reg_stall;
        }



        int reg_invalid = -1;


        List<PipelineRegister> registers    = new ArrayList<>();


        @override
        public boolean isForwardingResultValid(int index)
        {
            return registers.get(index).isForwardingResultValid();
        }

        @override
        public int getForwardingResultValue(int index)
        {
            return registers.get(index).getForwardingResultValue();
        }

        @override
        public boolean isForwardingResultValidNextCycle(int index)
        {
            return registers.get(index).isForwardingResultValidNextCycle();
        }

        public int getForwardingDestinationRegisterNumber(int index)
        {
            return registers.get(index).getForwardingDestinationRegisterNumber();
        }
        @override
        public void dumpForwardingData()
        {

            for (int i=1; i<registers.size(); i++)
            {


                int regnum = this.getForwardingDestinationRegisterNumber(i);
                if (regnum < 0)
                {
                    return;
                }
                else
                {
                    boolean valid = this.isForwardingResultValid(i);
                    if (valid)
                    {
                        int value = this.getForwardingResultValue(i);

                    }
                    else
                    {
                        return;
                    }
                }
            }
        }


        @Override
        public void compute(FetchToDecode input, DecodeToExecute output)
        {
            InstructionBase ins = input.getInstruction();

            int value0 = 0;
            wrong_reg_stall = false;


            reg_invalid = -1;


            while (ins.isNull()) return;


            GlobalData globals = (GlobalData)core.getGlobalResources();
            int[] regfile = globals.reg_file;
            boolean[] reginval = globals.register_invalid;

            EnumOpcode opcode = ins.getOpcode();
            boolean oper0src = opcode.oper0IsSource();

            Operand oper0 = ins.getOper0();
            Operand src1  = ins.getSrc1();
            Operand src2  = ins.getSrc2();



            int regnum0 = oper0.getRegisterNumber();

            if(oper0src)
            {

                if (oper0.isRegister())
                {
                    if (reginval[regnum0])
                    {
                        wrong_reg_stall = true;

                        return;
                    }
                    else
                    {

                        oper0.lookUpFromRegisterFile(regfile);

                    }
                }

                value0 = oper0.getValue();
            }


            if (src1.isRegister())
            {
                if (reginval[src1.getRegisterNumber()])
                {
                    wrong_reg_stall = true;

                    return;
                }
                else
                {
                    src1.lookUpFromRegisterFile(regfile);
                }
            }
            int value1 = src1.getValue();


            if (src2.isRegister())
            {
                if (reginval[src2.getRegisterNumber()])
                {
                    wrong_reg_stall = true;

                    return;
                }
                else
                {
                    src2.lookUpFromRegisterFile(regfile);
                }
            }





            boolean branch = false;
            int value2 = src2.getValue();
            InstructionBase null_ins = VoidInstruction.getVoidInstruction();

            switch (opcode)
            {
                case JMP:

                    if (ins.getLabelTarget().isNull()) {
                        globals.next_prgcount_YESbranch = value0;
                    }
                    else
                    {
                        globals.next_prgcount_YESbranch =
                                ins.getLabelTarget().getAddress();
                    }


                    globals.nxt_br_dec = GlobalData.EnumBranchState.YES;


                    output.setInstruction(null_ins);

                    return;


                case BRA:

                    switch (ins.getComparison())
                    {
                        case GT:
                            branch = (value0 > 0);
                            break;
                        case GE:

                            branch = (value0 >= 0);
                            break;
                        case EQ:
                            branch = (value0 == 0);
                            break;
                        case LT:
                            branch = (value0 < 0);
                            break;
                        case LE:
                            branch = (value0 <= 0);
                            break;


                        case NE:
                            branch = (value0 != 0);
                            break;



                    }

                    if (branch)
                    {

                        if (ins.getLabelTarget().isNull())
                        {
                            globals.next_prgcount_YESbranch = value1;
                        }
                        else
                        {
                            globals.next_prgcount_YESbranch =
                                    ins.getLabelTarget().getAddress();
                        }


                        globals.nxt_br_dec = GlobalData.EnumBranchState.YES;
                    }
                    else
                    {

                        globals.nxt_br_dec = GlobalData.EnumBranchState.NO;
                    }


                    output.setInstruction(null_ins);

                    return;




                case CALL:

                    return;
            }

            if(opcode.needsWriteback())
            {
                reg_invalid= regnum0;
            }


            output.setInstruction(ins);
        }


        @Override
        public void advanceClock()
        {

            while(nextStageCanAcceptWork() && reg_invalid>=0)
            {

                GlobalData globals = (GlobalData)core.getGlobalResources();
                boolean[] reginval = globals.register_invalid;


                reginval[reg_invalid] = true;

                reg_invalid = -1;
            }
        }
    }


    // Execute Stage
    public static class Execute extends PipelineStageBase<DecodeToExecute,ExecuteToMemory>
    {
        public Execute(CpuCore core, PipelineRegister input, PipelineRegister output)
        {
            super(core, input, output);
        }

        @Override
        public void compute(DecodeToExecute input, ExecuteToMemory output)
        {

            InstructionBase ins = input.getInstruction();
            while(ins.isNull()) return;


            int oper0 =   ins.getOper0().getValue();
            int source1 = ins.getSrc1().getValue();
            int source2 = ins.getSrc2().getValue();



            int X = MyALU.execute(ins.getOpcode(), source1, source2, oper0);


            output.result = X;


            output.setInstruction(ins);
        }
    }


    /*** Memory Stage ***/
    public static class Memory extends PipelineStageBase<ExecuteToMemory,MemoryToWriteback>
    {
        public Memory(CpuCore core, PipelineRegister input, PipelineRegister output)
        {
            super(core, input, output);
        }

        @Override
        public void compute(ExecuteToMemory input, MemoryToWriteback output)
        {
            int addr = input.result;
            int value;
            InstructionBase ins = input.getInstruction();
            while (ins.isNull()) return;


            GlobalData globals = (GlobalData)core.getGlobalResources();


            switch (ins.getOpcode())
            {
                case STORE:

                    value = ins.getOper0().getValue();
                    globals.memory[addr] = value;
                    break;



                case LOAD:

                    value = globals.memory[addr];


                    output.result = value;



                    break;

                default:
                    output.result = input.result;
                    break;




            }
            output.setInstruction(ins);
        }
    }


    // Writeback Stage
    public static class Writeback extends PipelineStageBase<MemoryToWriteback,VoidLatch>
    {
        public Writeback(CpuCore core, PipelineRegister input, PipelineRegister output)
        {
            super(core, input, output);
        }

        @Override
        public void compute(MemoryToWriteback input, VoidLatch output)
        {
            InstructionBase ins = input.getInstruction();
            while (ins.isNull()) return;
            GlobalData globals = (GlobalData)core.getGlobalResources();



            if (ins.getOpcode().needsWriteback())
            {

                int regnum = ins.getOper0().getRegisterNumber();

                globals.register_invalid[regnum] = false;
                globals.reg_file[regnum] = input.result;


                //globals.register_invalid[regnum] = false;

            }


            while (ins.getOpcode() == EnumOpcode.HALT)
            {
                globals.running = false;
            }
        }
    }
}
