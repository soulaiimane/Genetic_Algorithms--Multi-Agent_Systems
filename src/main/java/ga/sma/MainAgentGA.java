package ga.sma;

import ga.sequenciel.GAUtils;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.*;

public class MainAgentGA extends Agent {
    Random random=new Random();
    List<AgentFitness> agentsFitness=new ArrayList<>();
    @Override
    protected void setup() {
        DFAgentDescription dfAgentDescription=new DFAgentDescription();
        ServiceDescription serviceDescription=new ServiceDescription();
        serviceDescription.setType("ga");
        dfAgentDescription.addServices(serviceDescription);
        try {
            DFAgentDescription[] agentsDescriptions = DFService.search(this, dfAgentDescription);
            System.out.println(agentsDescriptions.length);
            for (DFAgentDescription dfAD:agentsDescriptions) {
                agentsFitness.add(new AgentFitness(dfAD.getName(),0));
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        calculateFitness();
        SequentialBehaviour sequentialBehaviour=new SequentialBehaviour();

        sequentialBehaviour.addSubBehaviour(new Behaviour() {
            int cpt=0;
            @Override
            public void action() {
                ACLMessage receivedMSG = receive();
                if (receivedMSG!=null){
                    cpt++;
                    System.out.println(cpt);
                    int fitness=Integer.parseInt(receivedMSG.getContent());
                    AID sender=receivedMSG.getSender();
                    System.out.println(sender.getName()+" "+fitness);
                    setAgentFitness(sender,fitness);
                    if(cpt== GAUtils.POPULATION_SIZE){
                        Collections.sort(agentsFitness,Collections.reverseOrder());
                        showPopulation();
                    }
                }else {
                    block();
                }
            }

            @Override
            public boolean done() {
                return cpt==GAUtils.POPULATION_SIZE;
            }

        });
        sequentialBehaviour.addSubBehaviour(new Behaviour() {
            int it=0;
            AgentFitness agent1;
            AgentFitness agent2;
            @Override
            public void action() {
                selection();
                crossover();
                Collections.sort(agentsFitness,Collections.reverseOrder());
                sendMessage("chromosome",agentsFitness.get(0).getAid(),ACLMessage.REQUEST);
                ACLMessage aclMessage=blockingReceive();
                System.out.println(it+" "+aclMessage.getContent()+" : "+agentsFitness.get(0).getFitness());
                it++;
            }

            private void crossover() {

                ACLMessage aclMessage1=blockingReceive();
                ACLMessage aclMessage2=blockingReceive();
                int crossoverPoint=random.nextInt(GAUtils.CHROMOSOME_SIZE-2);
                crossoverPoint++;
                char[] chromParent1=aclMessage1.getContent().toCharArray();
                char[] chromParent2=aclMessage2.getContent().toCharArray();
                char[] chromChild1=new char[GAUtils.CHROMOSOME_SIZE];
                char[] chromChild2=new char[GAUtils.CHROMOSOME_SIZE];
                for (int i=0;i<GAUtils.CHROMOSOME_SIZE;i++) {
                    chromChild1[i]=chromParent1[i];
                    chromChild2[i]=chromParent2[i];
                }
                for (int i=0;i<crossoverPoint;i++) {
                    chromChild1[i]=chromParent2[i];
                    chromChild2[i]=chromParent1[i];
                }

                int fitness=0;
                for (int i=0;i<GAUtils.CHROMOSOME_SIZE;i++) {
                    if(chromChild1[i]==GAUtils.SOLUTION.charAt(i))
                        fitness+=1;
                }

                agentsFitness.get(GAUtils.POPULATION_SIZE-2).setFitness(fitness);

                sendMessage(new String(chromChild1),agentsFitness.get(GAUtils.POPULATION_SIZE-2).getAid(),ACLMessage.REQUEST);
                sendMessage(new String(chromChild2),agentsFitness.get(GAUtils.POPULATION_SIZE-1).getAid(),ACLMessage.REQUEST);
                //mn b3d mansiftoh l individual ghayrje3 isiftlina fitness jdida
                ACLMessage receivedMsg1=blockingReceive();
                ACLMessage receivedMsg2=blockingReceive();
                //mn b3d matwselna fitness jdida kanbdloha
                setAgentFitness(receivedMsg1.getSender(),Integer.parseInt(receivedMsg1.getContent()));
                setAgentFitness(receivedMsg2.getSender(),Integer.parseInt(receivedMsg2.getContent()));
            }

            public void selection(){
                agent1=agentsFitness.get(0);
                agent2=agentsFitness.get(1);
                sendMessage("chromosome",agent1.getAid(),ACLMessage.REQUEST);
                sendMessage("chromosome",agent2.getAid(),ACLMessage.REQUEST);

            }
            @Override
            public boolean done() {
                return it>GAUtils.MAX_IT || agentsFitness.get(0).getFitness()==GAUtils.MAX_FITNESS;
            }
        });
        addBehaviour(sequentialBehaviour);

    }
private void sendMessage(String content,AID aid,int performative){

    ACLMessage aclMessage=new ACLMessage(performative);
    aclMessage.setContent(content);
    aclMessage.addReceiver(aid);
    send(aclMessage);
}
private void calculateFitness(){
    ACLMessage message=new ACLMessage(ACLMessage.REQUEST);

    for (AgentFitness agf:agentsFitness) {
        message.addReceiver(agf.getAid());
    }
    message.setContent("fitness");
    send(message);

}
private void setAgentFitness(AID aid, int fitness){
        for (int i=0;i<GAUtils.POPULATION_SIZE;i++){
            if(agentsFitness.get(i).getAid().equals(aid)){
                agentsFitness.get(i).setFitness(fitness);
                break;
            }
        }
}

private void showPopulation(){
    for (AgentFitness agentFitness:agentsFitness) {
        System.out.println(agentFitness.getAid().getName()+" "+agentFitness.getFitness());
    }
}
}
