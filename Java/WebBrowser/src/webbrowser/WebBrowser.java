/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webbrowser;

import java.awt.event.*;
import java.io.IOException;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * @author Yasaman
 */
class TextAreaExample {

}

public class WebBrowser {

    /**
     * @param args the command line arguments
     */
    JTextField proxyserver, proxyport;
    JTextArea response, request;

    WebBrowser() {
        JFrame f = new JFrame();

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
        JTextField protocol = new JTextField("UDP");
        protocol.setEditable(false);
        protocol.setBounds(30, 110, 200, 20);
        f.add(protocol);

        JLabel requestLabel = new JLabel("Request:");
        requestLabel.setBounds(30, 140, 200, 20);
        f.add(requestLabel);
        request = new JTextArea();
        JScrollPane requestScroll = new JScrollPane(request);
        requestScroll.setBounds(30, 160, 400, 100);
        f.add(requestScroll);

        JLabel responseLabel = new JLabel("Response:");
        responseLabel.setBounds(30, 270, 200, 20);
        f.add(responseLabel);
        response = new JTextArea("");
        response.setEditable(false);
        JScrollPane responseScroll = new JScrollPane(response);
        responseScroll.setBounds(30, 290, 800, 200);
        f.add(responseScroll);

        JButton submit = new JButton("Submit");
        submit.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Browse();
                } catch (ConnectException | UnknownHostException ex) {
                    JOptionPane.showMessageDialog(null, "Proxy server not found!", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (IOException ex) {
                    Logger.getLogger(WebBrowser.class.getName()).log(Level.SEVERE, null, ex);
                } catch (URISyntaxException ex) {
                    Logger.getLogger(WebBrowser.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        submit.setBounds(380, 500, 100, 20);
        f.add(submit);

        f.setResizable(false);
        f.setSize(860, 550);
        f.setLocationRelativeTo(null);
        f.setLayout(null);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    void Browse() throws SocketException, UnknownHostException, IOException, URISyntaxException {
        response.setText(null);
        byte[] receiveData;
        String requestString = request.getText();
        if (requestString.charAt(requestString.length() - 1) == '\n') {
            if (requestString.charAt(requestString.length() - 2) != '\n') {
                requestString += "\n";
            }
        } else {
            requestString += "\n\n";
        }
        requestString = requestString.replace("\n", "\r\n");
        String responseString = "";
        while (true) {
            DatagramSocket proxySocket = new DatagramSocket();
            DatagramPacket sendPacket = new DatagramPacket(requestString.getBytes(), requestString.getBytes().length, InetAddress.getByName(proxyserver.getText()), Integer.parseInt(proxyport.getText()));
            proxySocket.send(sendPacket);
            responseString = "";
            while (true) {
                receiveData = new byte[2000];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length, InetAddress.getByName(proxyserver.getText()), Integer.parseInt(proxyport.getText()));
                proxySocket.receive(receivePacket);
                if (receivePacket.getLength() == 0) {
                    break;
                }
                byte[] temp = new byte[receivePacket.getLength()];
                System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), temp, 0, receivePacket.getLength());
                responseString += new String(temp);
            }
            if (responseString.length() == 0) {
                JOptionPane.showMessageDialog(null, "Unknown Error!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (responseString.substring(0, 12).equals("HTTP/1.1 200")) {
                JOptionPane.showMessageDialog(null, "Webpage retrieved", "Success", JOptionPane.INFORMATION_MESSAGE);
                break;
            } else if (responseString.substring(0, 12).equals("HTTP/1.1 404")) {
                JOptionPane.showMessageDialog(null, "Page not found!", "Error", JOptionPane.ERROR_MESSAGE);
                break;
            } else if (responseString.substring(0, 12).equals("HTTP/1.1 301") || responseString.substring(0, 12).equals("HTTP/1.1 302")) {
                int start, end;
                start = responseString.lastIndexOf("Location: ");
                end = responseString.indexOf("\r\n", start);
                URI newLocation = new URI(responseString.substring(start + 10, end));
                if (newLocation.getPath().length() != 0) {
                    requestString = requestString.replaceFirst("GET (.*) HTTP", "GET " + newLocation.getPath() + " HTTP");
                }
                requestString = requestString.replaceFirst("Host: (.*)\r\n", "Host: " + newLocation.getHost() + "\r\n");
                //System.out.println(requestString);
                JOptionPane.showMessageDialog(null, "Redirecting to: " + responseString.substring(start + 10, end), "Redirect", JOptionPane.INFORMATION_MESSAGE);
            } else if (responseString.substring(0, 18).equals("Website not found!")) {
                JOptionPane.showMessageDialog(null, "Website not found!", "Error", JOptionPane.ERROR_MESSAGE);
                break;
            } else {
                JOptionPane.showMessageDialog(null, "Unknown Error!", "Error", JOptionPane.ERROR_MESSAGE);
                break;
            }
        }
        response.setText(responseString);
    }

    public static void main(String[] args) {
        new WebBrowser();
    }

}
