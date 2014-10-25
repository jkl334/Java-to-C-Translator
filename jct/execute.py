#!/usr/bin/python
import subprocess
import sys

try:
	if __name__ == "__main__":
		files = " ".join(sys.argv[1:])
		command = """ mvn compile && clear && mvn exec:java -Dexec.mainClass="nyu.segfault.Translator" -Dexec.args="{0}" """.format(files)
		subprocess.call(command,shell=True)
except:
		print("You did not enter a test file.")