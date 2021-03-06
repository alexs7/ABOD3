//  Instinct Server - TcpHandler
//  Copyright (c) 2016  Robert H. Wortham <r.h.wortham@gmail.com>
//	
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

package com.recklesscoding.abode.debugger.realtime.instinct;

import com.recklesscoding.abode.core.plan.Plan;
import com.recklesscoding.abode.core.plan.planelements.PlanElement;
import com.recklesscoding.abode.core.plan.planelements.action.ActionEvent;

import java.io.*;
import java.util.*;
import java.net.*;
import java.util.regex.*;

/**
 * @See: https://github.com/rwortham/Instinct-Server
 */
// class to handle comms over a particular tcp stream
public class TcpHandler implements Runnable {

    protected Socket clientSocket = null;
    protected String cmdFileName = null;
    protected PrintWriter oss = null;
    protected boolean logDisplay = false;
    protected boolean isStopped = false;
    protected HashMap<Integer, String> planElements = new HashMap<Integer, String>(255);
    protected HashMap<Integer, String> robotActions = new HashMap<Integer, String>(100);
    protected HashMap<Integer, String> robotSenses = new HashMap<Integer, String>(100);

    public TcpHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public TcpHandler(Socket clientSocket, String cmdFileName) {
        this.clientSocket = clientSocket;
        this.cmdFileName = cmdFileName;
    }

    // process command lines and send them to the client. Handles @ includefile directives
    // only return true if we are sending the command down to the robot
    public synchronized boolean sendCmd(String cmdLine) {
        if (cmdLine.startsWith("@")) // include file
        {
            String includeFileName = cmdLine.substring(cmdLine.indexOf("@") + 1).trim();
            sendFile(includeFileName);
            return true;
        } else {
            String cmd;
            if (cmdLine.startsWith(cmd = "PELEM"))
                addPlanElement(cmdLine.substring(cmd.length()).trim());
            else if (cmdLine.startsWith(cmd = "RACTION"))
                addRobotAction(cmdLine.substring(cmd.length()).trim());
            else if (cmdLine.startsWith(cmd = "RSENSE"))
                addRobotSense(cmdLine.substring(cmd.length()).trim());
            else // don't send PELEM, RACTION, RSENSE to the robot
            {
                oss.println(cmdLine);
                return true;
            }
        }
        return false;
    }

    public synchronized void toggleLogDisplay() {
        this.logDisplay = !this.logDisplay;
    }

    public void run() {
        try {
            BufferedReader iss = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            oss = new PrintWriter(clientSocket.getOutputStream(), true);

            sendFile(cmdFileName);

            // now loop, reading lines from the socket and writing them to the log file
            String line;
            // Create a Pattern object to search for certain output lines
            // after the timestamp, log lines starting with E, S, P, F, Z, R can be decoded
            // to resolve node, sense and action IDs to their respective names
            Pattern p = Pattern.compile("[ESPFZ]");
            String[] comparators = {"EQ", "NE", "GT", "LT", "TR", "FL"};

            while (!isStopped && (line = iss.readLine()) != null) {
                boolean bEcho = true; // default is to echo lines from the robot back to the console
                String[] elements = line.split("[ ]+");
                if ((elements.length > 3) && (elements[1].length() == 1)) // check its a single character
                {
                    if (elements[1].equals("X")) // this is a sensor log line
                    {
                        // not doing anything with these at present, but don't send to console
                        bEcho = logDisplay;
                    } else if (elements[1].equals("Y")) // this is a log line from the head cell matrix
                    {
                        // not doing anything with these at present, but don't send to console
                        bEcho = logDisplay;
                    } else if (elements[1].equals("R")) // this line is a log line from a releaser
                    {
                        bEcho = logDisplay;

                        try {
                            Integer val = new Integer(elements[2]);
                            String name = robotSenses.get(val);
                            if (!(name == null) && (name.length() > 0))
                                elements[2] = name;
                            val = new Integer(elements[3]); // this is the Comparator type
                            if (val < 6)
                                elements[3] = comparators[val];
                            line = "";
                            for (String str : elements) {
                                line = line + str + " ";
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Error processing Releaser log entry " + line);
                        }
                    } else {
                        Matcher m = p.matcher(elements[1]);
                        if (m.find()) // this is a log line from a plan element
                        {
                            bEcho = logDisplay;

                            try {
                                Integer val = new Integer(elements[3]);
                                String name = planElements.get(val);
                                if (!(name == null) && (name.length() > 0))
                                    elements[3] = name;

                                line = "";
                                for (String str : elements) {
                                    line = line + str + " ";
                                }

                                handlePlanElementUpdate(name, line);

                            } catch (NumberFormatException e) {
                                System.err.println("Error processing Element log entry " + line);
                            }
                        }
                    }
                }

                line = line.trim(); // remove any trailing spaces before logging

                if (bEcho)
                    System.out.println(line);

                // provide a way to close gracefully
                if (line.equals("bye")) {
                    System.out.println("bye received. Closing socket.");
                    break;
                }
            }

            if (!isStopped) {
                isStopped = true;
            }
        } catch (IOException e) {
            if (!isStopped)
                System.err.println("IO Exception caught: client disconnected.");
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {

            } // this is a bit poor I know
        }
    }

    private void handlePlanElementUpdate(String planElementName, String line) {
        String cvsSplitBy = " ";

        String[] splittedLine = line.split(cvsSplitBy);
        PlanElement planElement = null;
        String typeOfPlanElement;

        // Making sure the search is not pointless
        if (isValidLine(splittedLine)) {
            typeOfPlanElement = splittedLine[2];
            if (!isActionPatternElement(typeOfPlanElement)) { //We ignore ActionPatternELements as they are instinct only
                planElement = getPlanElement(typeOfPlanElement, planElement, planElementName);
                if (planElement != null) {
                    planElement.setToUpdate();
                }
            }
        }
    }

    public synchronized void stop() {
        this.isStopped = true;
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("IO Exception caught: client disconnect error.");
        }
    }


    // read a file and send it, minus comment lines, to the remote client
    protected void sendFile(String fileName) {
        try {
            if ((fileName != null) && (new File(fileName)).exists()) {
                // read the cmd file and write it to the robot
                BufferedReader ifs;
                ifs = new BufferedReader(new InputStreamReader(
                        new FileInputStream(new File(fileName))));

                String cmdLine;
                while ((cmdLine = ifs.readLine()) != null) {
                    if ((cmdLine.length() > 0) && !cmdLine.startsWith("//")) // lines starting with // are comments in the command file
                    {
                        if (sendCmd(cmdLine)) // if we sent the command to the robot then delay
                        {
                            // add a short delay to allow the robot to process each command, otherwise the robot's serial buffers overrun
                            try {
                                Thread.sleep(200); // limit to 5 commands per second
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }
                ifs.close();
            } else {
                System.err.println("Command file not found");
            }
        } catch (IOException e) {
            System.err.println("Exception caught: command file not processed");
        }
    }

    void addPlanElement(String str) {
        try {
            int idx = str.indexOf("=");
            String name = str.substring(0, idx);
            Integer value = new Integer(str.substring(idx + 1));
            // if plan is reloaded with different values, remove the old ones
            if (planElements.containsValue(value))
                planElements.remove(value);
            planElements.put(value, name);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            System.err.print("AddPlanElement, Invalid entry: ");
            System.err.println(str);
        }
    }

    void addRobotAction(String str) {
        try {
            int idx = str.indexOf("=");
            String name = str.substring(0, idx);
            Integer value = new Integer(str.substring(idx + 1));
            // if plan is reloaded with different values, remove the old ones
            if (robotActions.containsValue(value))
                robotActions.remove(value);
            robotActions.put(value, name);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            System.err.print("AddRobotAction, Invalid entry: ");
            System.err.println(str);
        }
    }

    void addRobotSense(String str) {
        try {
            int idx = str.indexOf("=");
            String name = str.substring(0, idx);
            Integer value = new Integer(str.substring(idx + 1));
            if (robotSenses.containsValue(value))
                robotSenses.remove(value);
            robotSenses.put(value, name);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            System.err.print("AddRobotSense, Invalid entry: ");
            System.err.println(str);
        }
    }

    private PlanElement getPlanElement(String typeOfPlanElement, PlanElement planElement, String planElementName) {
        if (isAction(typeOfPlanElement)) {
            planElement = Plan.getInstance().findAction(planElementName);
            if (planElement == null) {
                planElement = new ActionEvent(planElementName);
            }
        } else if (isActionPattern(typeOfPlanElement)) {
            planElement = Plan.getInstance().findActionPattern(planElementName);
            if (planElement == null) {
                planElement = new ActionEvent(planElementName);
            }
        } else if (isCompetence(typeOfPlanElement)) {
            planElement = Plan.getInstance().findCompetence(planElementName);
        } else if (isCompetenceElement(typeOfPlanElement)) {
            planElement = Plan.getInstance().findCompetenceElement(planElementName);
        } else if (isDrive(typeOfPlanElement)) {
            planElement = Plan.getInstance().findDriveCollection(planElementName);
        }
        return planElement;
    }

    private boolean isValidLine(String[] splittedLine) {
        return !splittedLine[0].startsWith("*") && splittedLine.length >= 4;
    }

    private boolean isActionPatternElement(String typeOfPlanElement) {
        return typeOfPlanElement.startsWith("APE");
    }

    private boolean isActionPattern(String typeOfPlanElement) {
        return typeOfPlanElement.equals("AP");
    }

    private boolean isAction(String typeOfPlanElement) {
        return typeOfPlanElement.equals("A");
    }

    private boolean isCompetence(String typeOfPlanElement) {
        return typeOfPlanElement.equals("C");
    }

    private boolean isCompetenceElement(String typeOfPlanElement) {
        return typeOfPlanElement.equals("CE");
    }

    private boolean isDrive(String typeOfPlanElement) {
        return typeOfPlanElement.equals("D");
    }
}
