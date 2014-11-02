#!/usr/bin/python
import os
import subprocess
import sys


# a script to run all test input through the java to c++ translator
# execute.py is a dependency

if sys.version_info[0] < 3:
    raise RunTimeError('python 3.0 or greater is required')

test_cases=os.listdir("./test_input");
case_name=""
for case in sorted(test_cases):
   print (case)
   subprocess.call("rm -rf test_input/*.cpp && rm -rf test_input/*.hpp",shell=True)
   subprocess.call("./execute.py test_input/"+case,shell=True)
   case_name=case.replace(".java","")
   subprocess.call("cat test_input/"+case_name+".cpp",shell=True)
   input("Press Enter to continue after finished viewing cpp file")
   subprocess.call("cat test_input/"+case_name+".hpp",shell=True)
   input("Press Enter to continue after finished viewing hpp output")

#clean out remaining output from test_input directory
subprocess.call("rm -rf test_input/*.cpp && rm -rf test_input/*.hpp",shell=True)





        
