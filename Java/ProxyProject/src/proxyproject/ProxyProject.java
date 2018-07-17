/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proxyproject;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.*;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author Yasaman
 */
class CacheEntry<E> {
    E response;
    Date expireDate;
    CacheEntry(E r) {
        response = r;
        Calendar cal = Calendar.getInstance();
        cal.setTime(Date.from(Instant.now()));
        cal.add(Calendar.SECOND,20);
        expireDate=cal.getTime();
    }
}
class UDPtoTCP {
    Map<String,CacheEntry<StringBuilder>> cache;
    
    UDPtoTCP(int p) throws SocketException, IOException {
        DatagramSocket clientSocket = new DatagramSocket(p);
        
        byte[] receiveData;
        byte[] sendData;
        
        cache = new HashMap<>();
        
        while(true) {

            receiveData = new byte[65000];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            byte[] temp = new byte[receivePacket.getLength()];
            System.arraycopy(receivePacket.getData(),receivePacket.getOffset(),temp,0,receivePacket.getLength());
            String sentence = new String(temp);
            InetAddress IPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();
            
            Iterator<Map.Entry<String,CacheEntry<StringBuilder>>> it = cache.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<String,CacheEntry<StringBuilder>> pair = it.next();
                if(pair.getValue().expireDate.before(Date.from(Instant.now()))) {
                    it.remove();
                }
            }
            
            if(sentence.contains("Host: ")) {
                if(cache.containsKey(sentence)) {
                    int counter;
                    int limit = 1000;
                    for(counter=limit; counter<cache.get(sentence).response.length(); counter+=limit) {
                        sendData = cache.get(sentence).response.substring(counter-limit,counter).getBytes();
                        clientSocket.send(new DatagramPacket(sendData, sendData.length, IPAddress, port));
                    }
                    sendData = cache.get(sentence).response.substring(counter-limit,cache.get(sentence).response.length()).getBytes();
                    clientSocket.send(new DatagramPacket(sendData, sendData.length, IPAddress, port));
                    clientSocket.send(new DatagramPacket(new byte[0], 0, IPAddress, port));
                    System.out.println("A Message has been recieved and responded from cache");
                    continue;
                }
                int start = sentence.lastIndexOf("Host: ");
                int end = sentence.indexOf("\n", start);
                String destination = sentence.substring(start+6, end-1);
                Socket serverSocket;
                try {
                    serverSocket = new Socket(destination,80);
                } catch(UnknownHostException e) {
                    String t = "Website not found!";
                    clientSocket.send(new DatagramPacket(t.getBytes(), t.getBytes().length, IPAddress, port));
                    clientSocket.send(new DatagramPacket(new byte[0], 0, IPAddress, port));
                    continue;
                }
                OutputStream outToServer = serverSocket.getOutputStream();
                InputStream inFromServer = serverSocket.getInputStream();
                outToServer.write(sentence.getBytes());
                outToServer.flush();
                serverSocket.shutdownOutput();
                int t;
                StringBuilder response = new StringBuilder();
                int counter = 0;
                int limit = 1000;
                while((t = inFromServer.read())!=-1) {
                    response.append((char)t);
                    counter++;
                    if(counter%limit==0 && counter>0) {
                        sendData = response.substring(counter-limit,counter).getBytes();
                        clientSocket.send(new DatagramPacket(sendData, sendData.length, IPAddress, port));
                    }
                }
                serverSocket.shutdownInput();
                serverSocket.close();
                sendData = response.substring((int) (Math.floor(counter/limit)*limit),counter).getBytes();
                clientSocket.send(new DatagramPacket(sendData, sendData.length, IPAddress, port));
                cache.put(sentence,new CacheEntry<>(response));
                System.out.println("A Message has been recieved and responded from online server");
            }
            clientSocket.send(new DatagramPacket(new byte[0], 0, IPAddress, port));

        }
    }
}
class TCPtoUDP {
    Map<String,CacheEntry<byte[]>> cache;
    
    TCPtoUDP(int p) throws SocketException, IOException {
        
        ServerSocket TCPSocket = new ServerSocket(p);
        
        cache = new HashMap<>();
        byte[] receiveData;
        
        while(true) {
            Socket clientSocket = TCPSocket.accept();
            OutputStream outToClient = clientSocket.getOutputStream();
            InputStream inFromClient = clientSocket.getInputStream();
            
            Iterator<Map.Entry<String,CacheEntry<byte[]>>> it = cache.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<String,CacheEntry<byte[]>> pair = it.next();
                if(pair.getValue().expireDate.before(Date.from(Instant.now()))) {
                    it.remove();
                }
            }
            
            int t;
            StringBuilder sentence = new StringBuilder();
            while((t = inFromClient.read())!=-1) {
                sentence.append((char)t);
            }
            System.out.println(sentence.toString());
            if(sentence.toString().contains("type=") && sentence.toString().contains("server=") && sentence.toString().contains("target=")) {
                
                String[] fields=sentence.toString().split(" ");
                int start,end;
                String type="",server="",target="";
                for(String s:fields) {
                    if(s.contains("type=")) {
                        type=s.substring(5);
                    } else if(s.contains("server=")) {
                        server=s.substring(7);
                    } else if(s.contains("target=")) {
                        target=s.substring(7);
                    }
                }
                String query = "";
                // Generate random transaction ID
                query += DatatypeConverter.printHexBinary(new byte[]{(byte)(Math.floor(Math.random()*256)),(byte)(Math.floor(Math.random()*256))});
                // Flags
                query += "0100"; 
                // Number of questions
                query += "0001";
                // Number of answer RR's
                query += "0000";
                // Number of authority RR's
                query += "0000";
                // Number of additional RR's
                query += "0000";
                // Adding name to query
                String[] targetName=target.split("\\.");
                for(String s:targetName) {
                    query += DatatypeConverter.printHexBinary(new byte[]{(byte)s.length()});
                    query += DatatypeConverter.printHexBinary(s.getBytes());
                }
                // End of name
                query += "00";
                // Type (A=0001 or CNAME=0005)
                if(type.equals("CNAME")) {
                    query += "0005";
                } else {
                    query += "0001";
                }
                // Class (IN = Internet)
                query += "0001";
                if(cache.containsKey(sentence.toString())) {
                    outToClient.write(cache.get(sentence.toString()).response,0,cache.get(sentence.toString()).response.length);
                    System.out.println("A Message has been recieved and responded from cache");
                } else {
                    byte[] sentence2 = DatatypeConverter.parseHexBinary(query);
                    receiveData = new byte[10000];
                    DatagramSocket serverSocket = new DatagramSocket();
                    DatagramPacket sendPacket = new DatagramPacket(sentence2, sentence2.length,InetAddress.getByName(server),53);
                    serverSocket.send(sendPacket);
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length,InetAddress.getByName(server),53);
                    serverSocket.receive(receivePacket);
                    byte[] temp = new byte[receivePacket.getLength()];
                    System.arraycopy(receivePacket.getData(),receivePacket.getOffset(),temp,0,receivePacket.getLength());
                    cache.put(sentence.toString(),new CacheEntry<>(temp));
                    outToClient.write(receivePacket.getData(),receivePacket.getOffset(),receivePacket.getLength());
                    System.out.println("A Message has been recieved and responded from online server");
                }
            }
            outToClient.flush();
            clientSocket.shutdownOutput();
            clientSocket.close();
        }
    }
}
public class ProxyProject {
    JComboBox<String> sourceprocotol,destinationprocotol;
    ProxyProject() {
        JFrame f= new JFrame();
        
        JLabel sourcehostLabel = new JLabel("Source Host:");
        sourcehostLabel.setBounds(30, 30, 200, 20);
        f.add(sourcehostLabel);
        JTextField sourcehost = new JTextField("127.0.0.1");
        sourcehost.setBounds(30, 50, 200, 20);
        sourcehost.setEnabled(false);
        f.add(sourcehost);
        
        JLabel sourceportLabel = new JLabel("Source Port:");
        sourceportLabel.setBounds(30, 70, 200, 20);
        f.add(sourceportLabel);
        JTextField sourceport = new JTextField("9876");
        sourceport.setBounds(30, 90, 200, 20);
        f.add(sourceport);
        
        JLabel sourceprocotolLabel = new JLabel("Source Protocol:");
        sourceprocotolLabel.setBounds(30, 110, 200, 20);
        f.add(sourceprocotolLabel);
        sourceprocotol = new JComboBox<>(new String[]{"UDP (for HTTP Client)","TCP (for DNS Client)"});
        sourceprocotol.setSelectedIndex(0);
        sourceprocotol.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(sourceprocotol.getSelectedIndex()==0) {
                    destinationprocotol.setSelectedIndex(1);
                } else {
                    destinationprocotol.setSelectedIndex(0);
                }
            }
        });
        sourceprocotol.setBounds(30, 130, 200, 20);
        f.add(sourceprocotol);
        
        JLabel destinationprocotolLabel = new JLabel("Destination Protocol:");
        destinationprocotolLabel.setBounds(30, 150, 200, 20);
        f.add(destinationprocotolLabel);
        destinationprocotol = new JComboBox<>(new String[]{"UDP","TCP"});
        destinationprocotol.setSelectedIndex(1);
        destinationprocotol.setBounds(30, 170, 200, 20);
        destinationprocotol.setEnabled(false);
        f.add(destinationprocotol);
        
        JButton submit = new  JButton("Start");
        submit.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            if(sourceprocotol.getSelectedIndex()==0) {
                                new UDPtoTCP(Integer.parseInt(sourceport.getText()));
                            } else {
                                new TCPtoUDP(Integer.parseInt(sourceport.getText()));
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(ProxyProject.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    
                }.start();
                sourceport.setEnabled(false);
                sourceprocotol.setEnabled(false);
                submit.setEnabled(false);
                submit.setText("Started!");
            }
        });
        submit.setBounds(80, 200, 100, 20);
        f.add(submit);
          
        f.setResizable(false);
        f.setSize(260,250);  
        f.setLocationRelativeTo(null);
        f.setLayout(null);  
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    /**
     * @param args the command line arguments
     * @throws java.net.SocketException
     */
    @SuppressWarnings("empty-statement")
    public static void main(String[] args) throws SocketException, IOException {
        // TODO code application logic here
        new ProxyProject();
    }
    
}
