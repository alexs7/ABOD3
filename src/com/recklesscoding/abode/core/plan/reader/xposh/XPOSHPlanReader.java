package com.recklesscoding.abode.core.plan.reader.xposh;

import com.recklesscoding.abode.core.plan.Plan;
import com.recklesscoding.abode.core.plan.planelements.PlanElement;
import com.recklesscoding.abode.core.plan.planelements.Sense;
import com.recklesscoding.abode.core.plan.planelements.action.ActionEvent;
import com.recklesscoding.abode.core.plan.planelements.action.ActionPattern;
import com.recklesscoding.abode.core.plan.planelements.competence.Competence;
import com.recklesscoding.abode.core.plan.planelements.competence.CompetenceElement;
import com.recklesscoding.abode.core.plan.planelements.drives.DriveCollection;
import com.recklesscoding.abode.core.plan.planelements.drives.DriveElement;
import com.recklesscoding.abode.core.plan.reader.PlanReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Author: @Andreas.
 * Date : @29/12/2015
 */
public class XPOSHPlanReader extends PlanReader {

    @Override
    public void readFile(String fileName) {
        readXPOSHFile(fileName);
    }

    private void readXPOSHFile(String fileName) {
        try {
            File fXmlFile = new File(fileName);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            actionPatternsCreator(doc);
            competenceElementsCreator(doc);
            competencesCreator(doc);
            driveElementCreator(doc);
            drivesCreator(doc);

            competenceElementsLinker(doc);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void actionPatternsCreator(Document doc) {
        NodeList actionPatternsNodes = doc.getElementsByTagName("ActionPattern");
        for (int i = 0; i < actionPatternsNodes.getLength(); i++) {
            if (actionPatternsNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element actionPatternElement = (Element) actionPatternsNodes.item(i);
                NodeList actionNodes = actionPatternElement.getElementsByTagName("Action");
                List<ActionEvent> actions = new ArrayList<>();
                for (int j = 0; j < actionNodes.getLength(); j++) {
                    if (actionNodes.item(j).getNodeType() == Node.ELEMENT_NODE) {
                        Element actionElement = (Element) actionNodes.item(j);
                        ActionEvent actionEvent = Plan.getInstance().findAction(actionElement.getAttribute("name"));
                        if (actionEvent == null) {
                            actionEvent = Plan.getInstance().createAction(actionElement.getAttribute("name"));
                        }
                        actions.add(actionEvent);
                    }
                }
                Plan.getInstance().addActionPattern(new ActionPattern(actionPatternElement.getAttribute("name"), actions));
            }
        }
    }

    private void competenceElementsCreator(Document doc) {
        NodeList competenceElementNodes = doc.getElementsByTagName("CompetenceElement");
        for (int i = 0; i < competenceElementNodes.getLength(); i++) {
            if (competenceElementNodes.item(i).getNodeType() == Node.ELEMENT_NODE &&
                    competenceElementNodes.item(i).getParentNode().getParentNode().getNodeName() != "Competence") {
                Element ceElement = (Element) competenceElementNodes.item(i);
                List<Sense> senses = conditionsCreator(ceElement.getElementsByTagName("Senses"));
                CompetenceElement competenceElement = new CompetenceElement(ceElement.getAttribute("name"),
                        senses);
                Plan.getInstance().addCompetenceElement(competenceElement);
            }
        }
    }


    private void competenceElementsLinker(Document doc) {
        NodeList competenceElementNodes = doc.getElementsByTagName("CompetenceElement");
        for (int i = 0; i < competenceElementNodes.getLength(); i++) {
            if (competenceElementNodes.item(i).getNodeType() == Node.ELEMENT_NODE &&
                    competenceElementNodes.item(i).getParentNode().getParentNode().getNodeName() != "Competence") {
                Element ceElement = (Element) competenceElementNodes.item(i);
                PlanElement triggered = Plan.getInstance().findActionPattern(ceElement.getAttribute("triggers"));
                if (triggered == null) {
                    triggered = Plan.getInstance().findCompetence
                            (ceElement.getAttribute("triggers"));
                }
                if (triggered == null) {
                    triggered = Plan.getInstance().createAction
                            (ceElement.getAttribute("triggers"));
                }
                Plan.getInstance().findCompetenceElementXPOSH(ceElement.getAttribute("name")).setTriggeredElement(triggered);
            }
        }
    }


    private void driveElementCreator(Document doc) {
        NodeList driveElementNode = doc.getElementsByTagName("DriveElement");
        for (int i = 0; i < driveElementNode.getLength(); i++) {
            if (driveElementNode.item(i).getNodeType() == Node.ELEMENT_NODE && driveElementNode.item(i).getParentNode().getParentNode().getNodeName() != "DriveElements") {
                Element deElement = (Element) driveElementNode.item(i);
                List<Sense> senses = conditionsCreator(deElement.getElementsByTagName("Senses"));
                String checkTime = deElement.getAttribute("checkTime");
                DriveElement driveElement;

                PlanElement triggered = Plan.getInstance().findActionPattern(deElement.getAttribute("triggers"));
                if (triggered == null) {
                    triggered = Plan.getInstance().findCompetence
                            (deElement.getAttribute("triggers"));
                }
                if (triggered == null) {
                    triggered = Plan.getInstance().createAction
                            (deElement.getAttribute("triggers"));
                }

                if (!checkTime.isEmpty()) {
                    Double.parseDouble(deElement.getAttribute("checkTime"));
                    driveElement = new DriveElement(deElement.getAttribute("name"), triggered,
                            senses, Double.parseDouble(deElement.getAttribute("checkTime")));
                } else {
                    driveElement = new DriveElement(deElement.getAttribute("name"), triggered,
                            senses);
                }

                Plan.getInstance().addDriveElement(driveElement);
            }
        }
    }

    private void competencesCreator(Document doc) {
        NodeList competenceElementNodes = doc.getElementsByTagName("Competence");

        for (int i = 0; i < competenceElementNodes.getLength(); i++) {
            if (competenceElementNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element competenceElement = (Element) competenceElementNodes.item(i);
                List<Sense> goals = conditionsCreator(competenceElement.getElementsByTagName("Senses"));
                List<CompetenceElement> competenceElements = ceCCreator(competenceElement.getElementsByTagName("CompetenceElements"));
                Competence competence = new Competence(competenceElement.getAttribute("name"),
                        goals, competenceElements);
                Plan.getInstance().addCompetence(competence);
            }
        }
    }

    private void drivesCreator(Document doc) {
        NodeList drivesNodes = doc.getElementsByTagName("Drive");

        for (int i = 0; i < drivesNodes.getLength(); i++) {
            if (drivesNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element driveElement = (Element) drivesNodes.item(i);
                List<Sense> conditions = conditionsCreator(driveElement.getElementsByTagName("Senses"));
                List<DriveElement> driveElements = deCCreator(driveElement.getElementsByTagName("DriveElements"));
                DriveCollection driveCollection = new DriveCollection(driveElement.getAttribute("name"),
                        conditions, driveElements);
                Plan.getInstance().addDriveCollection(driveCollection);
            }
        }
    }

    private List<Sense> conditionsCreator(NodeList conditionsNodes) {
        List<Sense> senses = new ArrayList<>();
        for (int i = 0; i < conditionsNodes.getLength(); i++) {
            if (conditionsNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element conditionsElement = (Element) conditionsNodes.item(i);
                NodeList conditionNodes = conditionsElement.getElementsByTagName("Sense");
                for (int j = 0; j < conditionNodes.getLength(); j++) {
                    if (conditionNodes.item(j).getNodeType() == Node.ELEMENT_NODE) {
                        Element conditionElement = (Element) conditionNodes.item(j);
                        Sense sense = Plan.getInstance().createSense(conditionElement.getAttribute("name"),
                                conditionElement.getAttribute("comperator"), conditionElement.getAttribute("value"));
                        senses.add(sense);
                    }
                }
                return senses;
            }
        }
        return senses;
    }

    private List<CompetenceElement> ceCCreator(NodeList cesNodes) {
        List<CompetenceElement> competenceElements = new ArrayList<>();
        for (int i = 0; i < cesNodes.getLength(); i++) {
            if (cesNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element cesElement = (Element) cesNodes.item(i);
                NodeList ceNodes = cesElement.getElementsByTagName("CompetenceElement");
                for (int j = 0; j < ceNodes.getLength(); j++) {
                    if (ceNodes.item(j).getNodeType() == Node.ELEMENT_NODE) {
                        Element ceElement = (Element) ceNodes.item(j);
                        CompetenceElement competenceElement = Plan.getInstance().
                                findCompetenceElementXPOSH(ceElement.getAttribute("name"));
                        if (competenceElement != null) {
                            competenceElements.add(competenceElement);
                        }
                    }
                }
                return competenceElements;
            }
        }
        return competenceElements;
    }

    private List<DriveElement> deCCreator(NodeList desNodes) {
        List<DriveElement> driveElements = new ArrayList<>();
        for (int i = 0; i < desNodes.getLength(); i++) {
            if (desNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element desNode = (Element) desNodes.item(i);
                NodeList deNode = desNode.getElementsByTagName("DriveElement");
                for (int j = 0; j < deNode.getLength(); j++) {
                    if (deNode.item(j).getNodeType() == Node.ELEMENT_NODE) {
                        Element deElement = (Element) deNode.item(j);
                        DriveElement driveElement = Plan.getInstance().
                                findDriveElementXPOSH(deElement.getAttribute("name"));
                        if (driveElement != null) {
                            driveElements.add(driveElement);
                        }
                    }
                }
                return driveElements;
            }
        }
        return driveElements;
    }
}