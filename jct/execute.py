#!/usr/bin/python
import subprocess
import sys

try:
	if __name__ == "__main__":
		command = """ mvn compile && clear && mvn exec:java -Dexec.mainClass="nyu.segfault.Translator" -Dexec.args="{0}" """.format(sys.argv[1])
		subprocess.call(command,shell=True)
except:
		print("You did not enter a test file.")