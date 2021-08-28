# Java Multi-threaded Virtual Terrain Generator
![GitHub code size](https://img.shields.io/github/languages/code-size/christopher-boustros/OS-Kernel-and-Shell-Simulator "Code size")
![GitHub license](https://img.shields.io/github/license/christopher-boustros/OS-Kernel-and-Shell-Simulator "License")

A command-line program written in C that simulates an operating system kernel and shell along with paging and CPU scheduling. This program was made as part of a course assignment for ECSE 427 Operating Systems in winter 2020 at McGill University.

This is a demo of the program executed from a Linux Terminal:
  
![Alt text](/Program_Demo.png?raw=true "Program Demo")
<br/><br/>

## How the program works

### Overview
Once the program is executed from a Bash command-line shell, it will display a simulated shell that accepts the following commands from the user:

```
help				            Displays all available commands

quit				            Exits the shell or the script with "Bye!"

clearmem			            Clears the shell memory

set VAR STRING			            Assigns STRING to variable VAR in shell memory

print VAR			            Displays the value assigned to variable VAR

run SCRIPT.TXT			            Executes the file SCRIPT.TXT

exec S1.TXT S2.TXT S3.TXT	            Executes up to three files concurrently
```

The user can enter a command into the program's shell, and it will display the output.

### Running text files
The program can run text files inside its working directory by entering the name of the file with the 'run' or 'exec' command. The program's working directory where it can open files is the directory where the program was executed from the Bash shell, which is not necessarily the directory where the program is located. Sample text files that can be executed by the program are available in the *tests* directory of this repository.

It is possible to execute a text file without the program's 'run' or 'exec' command by redirecting the output of the file to the program. If the name of the program is *mykernel* and the name of the text file is *script.txt*, then you can redirect the output of the file to the program with this command: `./mykernel < script.txt`. The program will start, execute the file line by line until redirection is finished, and then reopen its standard input to allow the user to enter commands.

### How files are executed using paging and CPU scheduling

If a file is executed from the program's shell with the 'run' command, the program will simply execute it line by line until it reaches the end of the file without using paging or CPU scheduling. If one or more files are executed with the 'exec' command, the program will simulate paging and CPU scheduling to execute the files concurrently. 

**Paging** is a memory management scheme used by an operating system to load data from secondary storage into random access memory (RAM). This is used to minimize the amount of RAM space used by a program by allowing the program to store its data in secondary storage and load a portion of that data into RAM only when it needs to be processed. The data loaded from secondary storage is divided into fixed-size blocks of virtual memory known as **pages**. Pages are loaded into fixed-size blocks of physical memory (RAM) known as **frames**. Each frame has a specific location in RAM and can hold exactly one page because the size of a frame is equal to the size of a page. The part of secondary storage used to store pages is known as the **backing store**. When one or more files are executed in the simulator with the 'exec' command, the program simulates this paging scheme by splitting up each file into page files, each with a size of four lines of text, and storing them in a backing store directory. Then, when a particular page file needs to be executed by the program, the four lines of text in that file are stored as strings in four consecutive elements of an array. The array represents the RAM and the block of four elements of the array represents a particular frame in RAM. When loading a page into RAM, the program first looks for an available frame in RAM to store the page; if there are no available frames, then it must select a **victim frame** to overwrite.

**CPU scheduling** is a technique performed by an operating system to execute multiple processes concurrently by allowing the central processing unit (CPU) to execute one process while pausing the execution of the other processes. Each process is executed for an equal amount of time known as a **quantum** before the CPU pauses its execution, which ensures that all concurrent processes are sharing the CPU resources equally. The processes waiting for the CPU to execute them or to resume their execution are stored as process control blocks in a round-robin queue known as the **ready queue**. A **process control block (PCB)** is a data structure that stores information about a process that the CPU is executing, such as which page of the process is currently being executed, which files are open for the process, and the registers that are used by the process. A PCB allows the information about the state of a process to be preserved when the CPU pauses its execution so that it can retrieve this information when execution of the process is resumed. In the simulator, files executed concurrently with the 'exec' command use a quantum of two lines of code, meaning that for each file, the CPU executes two lines of text before pausing the execution and moving on to the next file. This is shown in the demo image above for the command 'exec a.txt b.txt.'
<br/><br/>

## How to compile and run the program

### Requirements

- A Linux or macOS operating system is recommended. The program may not run correctly on Windows.
- A C compiler using the C11 / gnu11 standard, or similar
    - For Linux, you can use *gcc* from the GNU Project
    - For macOS, you can use the default compiler *Clang* from the LLVM Project (with alias gcc) or *gcc* from the GNU Project
    - For Windows, you can use *gcc* from MinGW
- A Bash command-line shell
    - For Linux and macOS, you can use the *Terminal*
    - For Windows, you can use *Git Bash*
    
### Optional tools
- GNU Make to compile and run the program with the Makefile
    - For Windows, you can use *Make* from *GnuWin32*
- Visual Studio Code with the C/C++ extension to view, edit, and debug the source code
    - Although the instructions below use Visual Studio Code, you may use any other C source-code editor or IDE for this project, such as Eclipse, NetBeans, or Code::Blocks

### Downloading this repository

To compile the program, you need a local copy of this repository on your computer. In the directory where you would like this repository to be located, clone this repository with `git clone https://github.com/christopher-boustros/OS-Kernel-and-Shell-Simulator.git`, or alternatively, download and extract the ZIP archive.

### <ins>Compiling and running the program with GNU Make</ins>

#### Directory structure

- The *`src`* directory contains the C source files with *.c* and *.h* extensions.

- The *`tests`* directory contains text files that can be executed by the program. This is the working directory of the program.

- The *`obj`* directory is made by the Makefile to store the object files with the *.o* extension compiled by gcc. This directory is not tracked by git.

- The *`bin`* directory is made by the Makefile to store the target binary program named *mykernel* compiled by gcc. This directory is not tracked by git.

#### Make commands

The Makefile defines the following commands. Open the root directory of this repository in a Bash shell to execute these commands.

###### `make`
This will compile the program if any changes have occurred to the source or object files. It will create the target program *mykernel* in the *bin* directory.

###### `make run`
This will run the program *mykernel* from the *bin* directory and set its working directory to the *tests* directory. If the program was not already compiled, it will compile it before running it.

###### `make clean`
This will remove all files from the *obj* and *bin* directories.
  
### <ins>Compiling and running the program with Visual Studio Code</ins>

Visual Studio Code with the C/C++ extension can be used instead of the Makefile to compile and run the program, as well as to view, edit, and debug the source code. 

In the root directory of this repository, you need to create two JSON configuration files: a `.vscode/tasks.json` file to compile the program and a `.vscode/launch.json` file to run it.

The following are example configuration files for a Linux operating system:

#### Example tasks configuration file
With `"cwd": "${workspaceFolder}/src"`, the working directory of the compiler is set to the *src* directory of the repository. This is where the compiler will look for the *.c* and *.h* source files.

`tasks.json`
``` json
{
    "tasks": [
        {
            "type": "cppbuild",
            "label": "C/C++: gcc build active file",
            "command": "/usr/bin/gcc",
            "args": [
                "-g",
                "main.c",
                "kernel.c",
                "shell.c",
                "interpreter.c",
                "shellmemory.c",
                "cpu.c",
                "pcb.c",
                "ram.c",
                "memorymanager.c",
                "-o",
                "${fileDirname}/mykernel"
            ],
            "options": {
                "cwd": "${workspaceFolder}/src"
            },
            "problemMatcher": [
                "$gcc"
            ],
            "group": {
                "kind": "build",
                "isDefault": true
            },
            "detail": "Task generated by Debugger."
        }
    ],
    "version": "2.0.0"
}
```

#### Example launch configuration file
With `"cwd": "${workspaceFolder}/tests"`, the working directory of the target program is set to the *tests* directory of the repository. This is where the program will look for files when it is running.

`launch.json`
``` json
{
    "version": "0.2.0",
    "configurations": [
        {
            "name": "gcc - Build and debug active file",
            "type": "cppdbg",
            "request": "launch",
            "program": "${fileDirname}/mykernel",
            "args": [],
            "stopAtEntry": false,
            "cwd": "${workspaceFolder}/tests",
            "environment": [],
            "externalConsole": false,
            "MIMode": "gdb",
            "setupCommands": [
                {
                    "description": "Enable pretty-printing for gdb",
                    "text": "-enable-pretty-printing",
                    "ignoreFailures": true
                }
            ],
            "preLaunchTask": "C/C++: gcc build active file",
            "miDebuggerPath": "/usr/bin/gdb"
        }
    ]
}
```
<br></br>

## License

This repository is released under the [MIT License](https://opensource.org/licenses/MIT) (see LICENSE).
