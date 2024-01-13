package org.example;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.example.KUtils.KConsumer;
import org.example.KUtils.KProducer;
import org.example.Messeging.Message;
import org.example.Messeging.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class UserInterface extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private JTextField messageFielddef;
    private JTextField usernameField;
    private JTextField usernameFielddef;
    private JTextField privatereceiverField;
    private JTextField privatereceiverFielddef;
    User user = new User("Anonymous");
    User receiver = new User("");
    KProducer producer = new KProducer();
    KConsumer consumer = new KConsumer(this.user.getName());
    
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public void appendToChat(String s){
        chatArea.append(s);
    }

    public UserInterface() {
        setTitle("Chat App");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        usernameFielddef = new JTextField("username");
        usernameField = new JTextField();
        usernameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setUserName();
            }
        });
        
        privatereceiverFielddef = new JTextField("private user");
        privatereceiverField = new JTextField();
        privatereceiverField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	setPrivateUserName();
            }
        });

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(usernameFielddef,gbc);
        
        gbc.ipadx = 200;
        gbc.gridx = 1;
        gbc.gridy = 0;
        inputPanel.add(usernameField,gbc); 
        
        gbc.ipadx = 0;
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(privatereceiverFielddef,gbc);
        
        gbc.ipadx = 200;
        gbc.gridx = 1;
        gbc.gridy = 1;
        inputPanel.add(privatereceiverField,gbc); 
        
        gbc.ipadx = 0;
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(sendButton,gbc); 
        
        gbc.ipadx = 200;
        gbc.gridx = 1;
        gbc.gridy = 2;
        inputPanel.add(messageField,gbc);
        
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(inputPanel, BorderLayout.SOUTH);
    }

    private void sendMessage() {
        String username = usernameField.getText();
        String receiver = privatereceiverField.getText();
        String message = messageField.getText();
        if (!username.isEmpty() && !message.isEmpty()) {
            Message m = new Message(message, username, receiver);
            producer.send(m);
            messageField.setText("");
        }
    }

    private void setUserName() {
        String username = usernameField.getText();
        if (!username.isEmpty()) {
            user.setName(username);
            chatArea.append("Username updated to: " + user.getName() + "\n\r");
         // Separate thread for running the consumer
            Runnable r = new Runnable(){
                @Override
                public void run() {
                	KafkaConsumer<String, String> privateconsumer = null;
                	try {
                		Properties properties = KConsumer.loadConfig("./client.properties");
                		privateconsumer = new KafkaConsumer<String, String>(properties);
                		String topicname = "messages_" + user.getName();
                        privateconsumer.subscribe(Collections.singleton(topicname));
                        while (!shutdown.get()){
                            ConsumerRecords<String, String> records = privateconsumer.poll(Duration.ofMillis(500));
                            records.forEach(record -> {
                            Message m =  new Message(record.value(), record.key(), record.timestamp());
                            String myMessege =   m.getTimestamp() + "private from: " + m.getFrom() + "\t| " + m.getContent() + "\n";
                            chatArea.append(myMessege);

                            });
                        }
                    } catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}finally {
                    	privateconsumer.close();
                        //counter.countDown();
                    }
                }           
            };
            Thread t1 = new Thread(r, "t1");
            t1.start();
        }
    }
    
    private void setPrivateUserName() {
        String privateusername = privatereceiverField.getText();
        if (!privateusername.isEmpty()) {
        	receiver.setName(privateusername);
            chatArea.append("Message will be sent only to: " + receiver.getName() + "\n\r");
        }
        else {
        	receiver.setName("");
        	chatArea.append("Message will be to everyone\n\r");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                UserInterface UI = new UserInterface();

                // Separate thread for running the consumer
                Thread consumerThread = new Thread() {
                    @Override
                    public void run() {
                        UI.consumer.run(UI);
                    }
                };
                consumerThread.start();
                UI.setVisible(true);
            }

        });
    }
}

