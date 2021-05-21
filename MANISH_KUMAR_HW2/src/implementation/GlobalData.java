
package implementation;

import utilitytypes.IGlobals;
import tools.InstructionSequence;


public class GlobalData implements IGlobals {
    @Override
    public void reset() {
        prgcount = 0;
        reg_file = new int[32];
    }



    boolean running;


    public InstructionSequence program;

    public static enum EnumBranchState {
        NONE, WAIT, YES, NO
    }


    public int[] reg_file = new int[32];
    public boolean[] register_invalid = new boolean[32];



    public int[] memory = new int[1024];




    public int prgcount = 0;

    public int next_prgcount_nobranch = 0;

    public int next_prgcount_YESbranch = 0;






    public EnumBranchState cur_br= EnumBranchState.NONE;



    public EnumBranchState nxt_br = EnumBranchState.NONE;


    public EnumBranchState nxt_br_dec = EnumBranchState.NONE;
}
