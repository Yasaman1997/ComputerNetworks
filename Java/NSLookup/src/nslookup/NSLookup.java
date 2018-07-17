/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nslookup;

import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author Yasaman & Melika
 */
public class NSLookup {

    /**
     * @param args the command line arguments
     */
    JTextField proxyserver,proxyport,server,target;
    JComboBox<String> type;
    JTextArea response;
    int k;
    
    NSLookup(){  
        JFrame f= new JFrame();
        
        JLabel proxyserverLabel = new JLabel("Proxy Server:");
        proxyserverLabel.setBounds(30, 10, 200, 20);
        f.add(proxyserverLabel);
        proxyserver = new JTextField("localhost");
        proxyserver.setBounds(30, 30, 200, 20);
        f.add(proxyserver);
        
        JLabel proxyportLabel = new JLabel("Proxy Port:");
        proxyportLabel.setBounds(30, 50, 200, 20);
        f.add(proxyportLabel);
        proxyport = new JTextField("9876");
        proxyport.setBounds(30, 70, 200, 20);
        f.add(proxyport);
        
        JLabel protocolLabel = new JLabel("Protocol:");
        protocolLabel.setBounds(30, 90, 200, 20);
        f.add(protocolLabel);
        JTextField protocol = new JTextField("TCP");
        protocol.setEditable(false);
        protocol.setBounds(30, 110, 200, 20);
        f.add(protocol);
        
        JLabel typeLabel = new JLabel("Type:");
        typeLabel.setBounds(30, 130, 200, 20);
        f.add(typeLabel);
        type = new JComboBox<>(new String[]{"A","CNAME"});
        type.setBounds(30, 150, 150, 20);
        f.add(type);
        
        JLabel serverLabel = new JLabel("Server:");
        serverLabel.setBounds(30, 170, 200, 20);
        f.add(serverLabel);
        server = new JTextField();
        server.setBounds(30, 190, 190, 20);
        f.add(server);
        
        JLabel targetLabel = new JLabel("Target:");
        targetLabel.setBounds(30, 210, 200, 20);
        f.add(targetLabel);
        target = new JTextField();
        target.setBounds(30, 230, 200, 20);
        f.add(target);
        
        JLabel responseLabel = new JLabel("Response:");
        responseLabel.setBounds(30, 260, 200, 20);
        f.add(responseLabel);
        response = new JTextArea("");
        response.setEditable(false);
        JScrollPane responseScroll = new JScrollPane(response);
        responseScroll.setBounds(30, 280, 800, 200);
        f.add(responseScroll);
        
        JButton submit = new  JButton("Submit");
        submit.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Lookup();
                } catch (IOException ex) {
                    Logger.getLogger(NSLookup.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        submit.setBounds(380, 500, 100, 20);
        f.add(submit);
        
        f.setResizable(false);
        f.setSize(860,550);  
        f.setLocationRelativeTo(null);
        f.setLayout(null);  
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    String DecompressName(String r) {
        String temp = "";
        boolean flag = false;
        do {
            if(r.charAt(k)>='C') {
                int pointer = Integer.parseInt((r.charAt(k)-'C')+r.substring(k+1,k+4),16);
                int z=pointer;
                do {
                    int count = Integer.parseInt(r.substring(2*z,2*(z+1)),16);
                    if(count==0) {
                        k+=4;
                        flag=true;
                        break;
                    }
                    temp += new String(DatatypeConverter.parseHexBinary(r.substring(2*(z+1),2*(z+count+1))))+".";
                    z+=(count+1);
                } while(true);
            } else {
                int count = Integer.parseInt(r.substring(k,k+2),16);
                if(count==0) {
                    k+=2;
                    flag=true;
                    break;
                }
                temp += new String(DatatypeConverter.parseHexBinary(r.substring(k+2,k+2*(count+1))))+".";
                k+=2*(count+1);
            }
            if(flag) {
                break;
            }
        } while(true);
        if(temp.length()>0) {
            temp=temp.substring(0,temp.length()-1);;
        } else {
            temp="<Root>";
        }
        return temp;
    }
    void Lookup() throws IOException {
        Socket proxySocket = null;
        try {
            proxySocket = new Socket(proxyserver.getText(),Integer.parseInt(proxyport.getText()));
            OutputStream outToProxy = proxySocket.getOutputStream();
            InputStream inFromProxy = proxySocket.getInputStream();
            outToProxy.write(("type="+type.getSelectedItem()+" server="+server.getText()+" target="+target.getText()).getBytes());
            outToProxy.flush();
            proxySocket.shutdownOutput();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int t;
            while((t = inFromProxy.read())!=-1) {
                buffer.write((byte)t);
            }
            String responseFromProxy = DatatypeConverter.printHexBinary(buffer.toByteArray());
            if(responseFromProxy.length()==0) {
                JOptionPane.showMessageDialog(null, "Unkown Error!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            //System.out.println(responseFromProxy);
            String responseText = "";

            String transactionID = responseFromProxy.substring(0,4);
            String flags = Integer.toBinaryString(Integer.parseInt(responseFromProxy.substring(4,8),16));
            if(flags.charAt(5)=='1') {
                responseText += "Server is an authority for domain\r\n";
            } else {
                responseText += "Server is not an authority for domain\r\n";
            }
            if(flags.substring(12, 16).equals("0011")) {
                responseText += "#ERROR#\r\nServer can't find your desired domain name\r\n";
            }
            int numberOfQuestions = Integer.parseInt(responseFromProxy.substring(8,12),16);
            int numberOfAnswers = Integer.parseInt(responseFromProxy.substring(12,16),16);
            int numberOfAuthorities = Integer.parseInt(responseFromProxy.substring(16,20),16);
            int numberOfAdditionals = Integer.parseInt(responseFromProxy.substring(20,24),16);
            //System.out.println(numberOfQuestions+" "+numberOfAnswers+" "+numberOfAuthorities+" "+numberOfAdditionals);
            k=24;
            for(int i=0; i<numberOfQuestions; i++) {
                String name = DecompressName(responseFromProxy);
                String typeID = responseFromProxy.substring(k,k+4);
                k+=4;
                String classID = responseFromProxy.substring(k,k+4);
                k+=4;
                //System.out.println("Question #"+(i+1));
                //System.out.println("Domain Name: "+name);
                //System.out.println("typeID: "+typeID);
                //System.out.println("classID: "+classID);
            }
            responseText += "\r\n";
            for(int i=0; i<numberOfAnswers; i++) {
                String name = DecompressName(responseFromProxy);
                String typeID = responseFromProxy.substring(k,k+4);
                k+=4;
                String classID = responseFromProxy.substring(k,k+4);
                k+=4;
                int TTL = Integer.parseInt(responseFromProxy.substring(k,k+8),16);
                k+=8;
                int dataLength = Integer.parseInt(responseFromProxy.substring(k,k+4),16);
                k+=4;
                String Address = "";
                for(int j=0; j<dataLength; j++) {
                    Address += Integer.parseInt(responseFromProxy.substring(k,k+2),16)+".";
                    k+=2;
                }
                Address=Address.substring(0,Address.length()-1);
                //System.out.println("Answer #"+(i+1));
                //System.out.println("Domain Name: "+name);
                //System.out.println("typeID: "+typeID);
                //System.out.println("classID: "+classID);
                //System.out.println("TTL: "+TTL);
                //System.out.println("dataLength: "+dataLength);
                //System.out.println("Address: "+Address);
                responseText += "Domain Name: "+name+"\r\n";
                responseText += "Address: "+Address+"\r\n";
            }
            responseText += "\r\n";
            for(int i=0; i<numberOfAuthorities; i++) {
                String name = DecompressName(responseFromProxy);
                String typeID = responseFromProxy.substring(k,k+4);
                k+=4;
                String classID = responseFromProxy.substring(k,k+4);
                k+=4;
                int TTL = Integer.parseInt(responseFromProxy.substring(k,k+8),16);
                k+=8;
                int dataLength = Integer.parseInt(responseFromProxy.substring(k,k+4),16);
                k+=4;
                String primaryNameServer = DecompressName(responseFromProxy);
                String mailAddress = DecompressName(responseFromProxy);
                String serialNumber = responseFromProxy.substring(k,k+8);
                k+=8;
                int refreshInterval = Integer.parseInt(responseFromProxy.substring(k,k+8),16);
                k+=8;
                int retryInterval = Integer.parseInt(responseFromProxy.substring(k,k+8),16);
                k+=8;
                int expireLimit = Integer.parseInt(responseFromProxy.substring(k,k+8),16);
                k+=8;
                int minimumTTL = Integer.parseInt(responseFromProxy.substring(k,k+8),16);
                k+=8;
    //            System.out.println("Authority #"+(i+1));
    //            System.out.println("Domain Name: "+name);
    //            System.out.println("typeID: "+typeID);
    //            System.out.println("classID: "+classID);
    //            System.out.println("TTL: "+TTL);
    //            System.out.println("dataLength: "+dataLength);
    //            System.out.println("Primary Name Server: "+primaryNameServer);
    //            System.out.println("Mailbox Address: "+mailAddress);
    //            System.out.println("Serial Number: "+serialNumber);
    //            System.out.println("Refresh Interval: "+refreshInterval);
    //            System.out.println("Retry Interval: "+retryInterval);
    //            System.out.println("Expire Limit: "+expireLimit);
    //            System.out.println("Minimum TTL: "+minimumTTL);
                responseText += "Domain Name: "+name+"\r\n";
                responseText += "Primary Name Server: "+primaryNameServer+"\r\n";
                responseText += "Mailbox Address: "+mailAddress+"\r\n";
            }
            responseText += "\r\n";
            proxySocket.shutdownInput();
            proxySocket.close();
            response.setText(responseText);
        }catch(UnknownHostException | ConnectException e) {
            JOptionPane.showMessageDialog(null, "Proxy server not found!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    public static void main(String[] args) {
        new NSLookup();
    }
    
}
