package implementation;





import cpusimulator.CpuSimulator;
import java.util.EnumSet;
import baseclasses.Latch;
import baseclasses.InstructionBase;
import voidtypes.VoidInstruction;
import utilitytypes.Operand;
import utilitytypes.IGlobals;
import utilitytypes.ICpuCore;
import utilitytypes.EnumOpcode;
import utilitytypes.Logger;
import baseclasses.PipelineStageBase;
import static utilitytypes.EnumOpcode.FSUB;
import static utilitytypes.EnumOpcode.FCMP;
import static utilitytypes.EnumOpcode.FADD;
import java.util.ArrayList;


	
	 
    public class IssueQueue extends PipelineStageBase
    {
 	   public IssueQueue(ICpuCore core)
 	   {
            super(core, "IssueQueue");
        }        
 	  static final EnumSet<EnumOpcode> floatAddSubSet = 
             EnumSet.of(FSUB,  FCMP, FADD);

 	   @Override
        public String getStatus() 
 	   {
 		  String m = super.getStatus();
          IGlobals globals = (GlobalData)getCore().getGlobals();
           
            return m;
        }
        
 	 
 	   
 	   @Override
 	   public void compute()
 	   {
 		  
 		  IGlobals globals = (GlobalData)getCore().getGlobals();
 		   ArrayList<Latch> Issue_Queue_list = (ArrayList<Latch>) globals.getPropertyObject("issue_queue");
 	 	   String Issue_Queue_String = ("IQ_list");
 	 	   Latch input1 = this.readInput(0).duplicate();
 		   InstructionBase ins = input1.getInstruction();
 		   
 	   
 	 
 	   if(ins.isNull() && Issue_Queue_list.size() == 0)
 	   {
 		   setActivity(Issue_Queue_String);
 		   return;
 	   }
 	   else
 	   {
 		  ArrayList<Latch> selectedList = new ArrayList<Latch>(512);
 		   EnumOpcode opcode = ins.getOpcode();
 		   
 		   int Index = -1;
 		   
 		   if(!ins.isNull())
 		   {
 			  Issue_Queue_list.add(input1);
 		   }
 		   
 		    int m =0;
 		   do
 		   {
 			  
 				   int n = 0;
 			   while(n<Issue_Queue_list.size())
 			   {
 				   if(Issue_Queue_list.get(m).getInstruction().getPCAddress() == Issue_Queue_list.get(n).getInstruction().getPCAddress())
 				   {
 					   while(m != n)
 					   {
 						  Issue_Queue_list.remove(n);
 					   }
 				   }
 				   n++;
 			   }
 			   m++;
 		   }
 		   while(m<Issue_Queue_list.size());
 		   
 		   int l = 0;
 		   do
 		   {
 			  int[] source_register = new int[10];
 			   boolean depend = false;
 			   Latch passing = Issue_Queue_list.get(l);
 			
 			   forwardingSearch(passing);
 			  EnumOpcode myOpcode = passing.getInstruction().getOpcode(); 
 			  Operand oper0 = passing.getInstruction().getOper0();
 			   Operand source1 = passing.getInstruction().getSrc1();
 			   Operand source2 = passing.getInstruction().getSrc2();
 			  Operand[] operArray = {oper0, source1, source2};
 			  
 			 source_register[0] = opcode.oper0IsSource() ? oper0.getRegisterNumber() : -1;
 			 source_register[1] = source1.getRegisterNumber();
 			source_register[2] = source2.getRegisterNumber();
 	            
 	            
 	          
 	           for (int sn=0; sn<3; sn++) 
 	            
 	            
 	            {
 	                int srcRegNum = source_register[sn];
 	               
 	                if (srcRegNum < 0) continue;
 	               
 	                if (operArray[sn].hasValue()) continue;
 	                
 	                String propname = "forward" + sn;
 	                if(!passing.hasProperty(propname))
 	                {
 	                	depend= true;
 	                	break;
 	                }
 	               
 		          }
 	            
 	            
 	           if (CpuSimulator.printForwarding) 
 	           {
 	        	  
 	                for (int sn=0; sn<3; sn++) 
 	                {
 	                    String propname = "forward" + sn;
 	                    if (input1.hasProperty(propname)) 
 	                    {
 	                    	
 	                    	 String srcRegName = operArray[sn].getRegisterName();
 	                       
 	                        String source_In = input1.getPropertyString(propname);
 	                        
 	                       String operand_name = PipelineStageBase.operNames[sn];
 	                       
 	                       Logger.out.printf("# Posting forward %s from %s to %s next stage\n", 
 	                    		  source_register,
 	                                source_In, operand_name);
 	                              
 	                    }
 	                }
 	            }
 	            
 	             Issue_Queue_String += " \n";
 	           Issue_Queue_String += Issue_Queue_list.get(l).getInstruction().toString();
 	             
 	          
	            if(!input1.getInstruction().isNull() && l == Issue_Queue_list.size()-1)
	            {
	            	Issue_Queue_String +=" recent ";
	            }
 	            if(!depend)
 	            {
 	            	selectedList.add(Issue_Queue_list.get(l).duplicate());
 	            	Issue_Queue_String +=" chose ";
 	            }
 	          
 	          
 	         l++;   
 		   }
 		while(l<Issue_Queue_list.size());
 		   
 		   setActivity(Issue_Queue_String);
 		   
 		  
 		   
 	  
 		   
 		  if(selectedList.size() != 0)
 		   {
 			 
 			  int n = 0;
 			  do
 			   {
 				 Latch selectedInput;
 				 selectedInput = selectedList.get(n);
 				   int output_num;
 				   Latch output;
 				   
 				  
 				 ins = selectedInput.getInstruction();
 				   opcode = ins.getOpcode();
 				  
 				  if (opcode == EnumOpcode.FMUL) 
		            {
		                output_num = lookupOutput("IQToFloatMul");
		                output = this.newOutput(output_num);
		            } 
 				  else
 					 if (opcode == EnumOpcode.MUL)
  		            {
  		                output_num = lookupOutput("IQToIntMul"); 
  		                output = this.newOutput(output_num);
  		            } 
 					 else
 						 
 						 if (opcode == EnumOpcode.DIV || opcode == EnumOpcode.MOD) 
 	 		            {
 	 		                output_num = lookupOutput("IQToIntDiv");
 	 		                output = this.newOutput(output_num);
 	 		            }
 				  
 						 else
 							 
 							 if (opcode == EnumOpcode.FDIV) 
 		 		            {
 		 		                output_num = lookupOutput("IQToFloatDiv");
 		 		                output = this.newOutput(output_num);
 		 		            } 
 							 
 							 else
 		 		            
 				   if (floatAddSubSet.contains(opcode))
 				   {
 		                output_num = lookupOutput("IQToFloatAddSub");
 		                output = this.newOutput(output_num);
 		            }
 				   
 				   else
 		           
 		           
 		            
 		            if (opcode.accessesMemory())
 		            {
 		                output_num = lookupOutput("IQToMemory");
 		                output = this.newOutput(output_num);
 		            } 
 		            else 
 		            {
 		                output_num = lookupOutput("IQToExecute");
 		                output = this.newOutput(output_num);
 		            }
 				   
 				   
 		            while (!output.canAcceptWork()) return;
 		            
 		           
 		           for(int r = 0; r< Issue_Queue_list.size(); r++)
 		          
 		          
 		            {
 		            	if(selectedList.get(n).getInstruction().getPCAddress() == Issue_Queue_list.get(r).getInstruction().getPCAddress())
 		            	{
 		            		Issue_Queue_list.remove(r);
 		            	}
 		            
 		            }
 		            getCore().incDispatched();
 		            output.setInstruction(ins);
 		             input1.consume();
 		            output.write();
 		            output.copyAllPropertiesFrom(selectedInput);
 		             n++;
 			   }
 		      while(n<selectedList.size());
 		   
 	   }
 		   
 		   
 		   else
 		   {
 			  int output_num;
 			 input1.consume();
 			  Latch selectedInput = input1;
 			  ins = VoidInstruction.getVoidInstruction();
 			  Latch output;
 			  output_num = lookupOutput("IQToExecute");
 			  
 			 output = this.newOutput(output_num);
 			  while(!output.canAcceptWork()) 
 				 
 				  return;
 			 output.copyAllPropertiesFrom(selectedInput);
 			 output.setInstruction(ins);
 				 output.write();
 			  
 			 
 			 
 			 
 		   }
 	   }
 	   }
    }
     


