/* SendMail.res.layout.main.xml */  
<?xml version="1.0" encoding="utf-8"?>   
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"  
    android:orientation="vertical"  
    android:layout_width="fill_parent"  
    android:layout_height="fill_parent"  
    >   
<TextView     
    android:layout_width="fill_parent"    
    android:layout_height="wrap_content"    
    android:text="@string/hello"  
    />          
     <Button   
        android:id="@+id/send"         
        android:layout_width="wrap_content"  
        android:layout_height="wrap_content"  
        android:layout_gravity="center_horizontal"     
        android:text="@string/sendemail"               
    ></Button>   
</LinearLayout>   
/*                                 */  
/* SendMail.res.values.strings.xml */  
/*                                 */  
<?xml version="1.0" encoding="utf-8"?>   
<resources>   
    <string name="hello">Hello World, SendMail!</string>   
    <string name="app_name">SendMail</string>   
    <string name="sendemail">Send the eMail</string>   
       
</resources>   
/*                                 */  
/* SendMail.AndroidManifest.xml    */  
/*                                 */  
<?xml version="1.0" encoding="utf-8"?>   
<manifest xmlns:android="http://schemas.android.com/apk/res/android"  
      package="com.example.sendmail"  
      android:versionCode="1"  
      android:versionName="1.0">   
      <uses-permission android:name="android.permission.INTERNET" />   
    <application android:icon="@drawable/icon" android:label="@string/app_name">   
        <activity android:name=".SendMail"  
                  android:label="@string/app_name">   
            <intent-filter>   
                <action android:name="android.intent.action.MAIN" />   
                <category android:name="android.intent.category.LAUNCHER" />   
            </intent-filter>   
        </activity>   
  
    </application>   
    <uses-sdk android:minSdkVersion="3" />   
  
</manifest>    
/*                                 */  
/* Sendmail.src.com.example.sendmail.SendMail.java   */  
/*                                 */  
package com.example.sendmail;   
  
//package org.apache.android.mail;   
  
import android.app.Activity;   
import android.os.Bundle;   
import android.util.Log;   
import android.view.View;   
import android.widget.Button;   
  
  
public class SendMail extends Activity {   
    /**  
     * Called with the activity is first created.  
     */  
    @Override  
    public void onCreate(Bundle icicle) {   
        super.onCreate(icicle);   
        setContentView(R.layout.main);   
        final Button send = (Button) this.findViewById(R.id.send);   
           
        send.setOnClickListener(new View.OnClickListener() {   
            public void onClick(View view) {   
            GMailSender sender = new GMailSender("gmailuserid","password"); // SUBSTITUTE HERE                     
                try {   
                    sender.sendMail(   
                            "about Hello World by email",   //subject.getText().toString(),    
                            "body of the world ",           //body.getText().toString(),    
                            "anybody@hotmail.com",          //from.getText().toString(),   
                            "somebody@gmail.com"            //to.getText().toString()   
                            );   
                } catch (Exception e) {   
                    Log.e("SendMail", e.getMessage(), e);   
                }   
            }   
        });   
    }   
}   
/*                                 */  
/* Sendmail.src.com.example.sendmail.GMailSender.java   */  
/*                                 */  
package com.example.sendmail;   
  
import java.io.ByteArrayInputStream;   
import java.io.IOException;   
import java.io.InputStream;   
import java.io.OutputStream;   
import java.security.Security;   
import java.util.Properties;   
  
import javax.activation.DataHandler;   
import javax.activation.DataSource;   
import javax.mail.Message;   
import javax.mail.PasswordAuthentication;   
import javax.mail.Session;   
import javax.mail.Transport;   
import javax.mail.internet.InternetAddress;   
import javax.mail.internet.MimeMessage;   
  
public class GMailSender extends javax.mail.Authenticator {   
    private String mailhost = "smtp.gmail.com";   
    private String user;   
    private String password;   
    private Session session;   
  
  //  static {   
   //    Security.addProvider(new org.apache.harmony.xnet.provider.jsse.JSSEProvider());   
  // }   
  
    public GMailSender(String user, String password) {   
        this.user = user;   
        this.password = password;   
  
        Properties props = new Properties();   
        props.setProperty("mail.transport.protocol", "smtp");   
        props.setProperty("mail.host", mailhost);   
        props.put("mail.smtp.auth", "true");   
        props.put("mail.smtp.port", "465");   
        props.put("mail.smtp.socketFactory.port", "465");   
        props.put("mail.smtp.socketFactory.class",   
                "javax.net.ssl.SSLSocketFactory");   
        props.put("mail.smtp.socketFactory.fallback", "false");   
        props.setProperty("mail.smtp.quitwait", "false");   
  
        session = Session.getDefaultInstance(props, this);   
    }   
  
    protected PasswordAuthentication getPasswordAuthentication() {   
        return new PasswordAuthentication(user, password);   
    }   
  
    public synchronized void sendMail(String subject, String body, String sender, String recipients) throws Exception {   
        MimeMessage message = new MimeMessage(session);   
        DataHandler handler = new DataHandler(new ByteArrayDataSource(body.getBytes(), "text/plain"));   
        message.setSender(new InternetAddress(sender));   
        message.setSubject(subject);   
        message.setDataHandler(handler);   
        if (recipients.indexOf(',') > 0)   
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));   
        else  
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipients));   
        Transport.send(message);   
    }   
  
    public class ByteArrayDataSource implements DataSource {   
        private byte[] data;   
        private String type;   
  
        public ByteArrayDataSource(byte[] data, String type) {   
            super();   
            this.data = data;   
            this.type = type;   
        }   
  
        public ByteArrayDataSource(byte[] data) {   
            super();   
            this.data = data;   
        }   
  
        public void setType(String type) {   
            this.type = type;   
        }   
  
        public String getContentType() {   
            if (type == null)   
                return "application/octet-stream";   
            else  
                return type;   
        }   
  
        public InputStream getInputStream() throws IOException {   
            return new ByteArrayInputStream(data);   
        }   
  
        public String getName() {   
            return "ByteArrayDataSource";   
        }   
  
        public OutputStream getOutputStream() throws IOException {   
            throw new IOException("Not Supported");   
        }   
    }   
}   
/*                    */  
/* comments and notes */  
This program sends an email programmatically without user intervention.    
It is based on the excellent article at    
http://davanum.wordpress.com/2007/12/22/android-send-email-via-gmail-actually-via-smtp/   
(many thanks davanum!)   
  
You need to use a valid gmail account id and its password.   
  
The real trick is to get the javax components working.   
Download them from   
http://javamail-android.googlecode.com/files/mail.jar   
http://javamail-android.googlecode.com/files/activation.jar   
http://javamail-android.googlecode.com/files/additionnal.jar   
  
into a folder called for example mailJars   
  
In Eclipse right click on the project in the package explorer (left column).   
In the new window Properties for sendmail, choose Java Build Path    
Add External JARs... These will then appear in a new folder in the project    
"Referenced Libraries", with their names and a reference to the full path    
of their folder.   
  
Notes on the program....   
1) This is a step towards implementing an automatic email forwarding program    
to send pictures to an email address rather than uploading them through HTTP.    
If you are interested in doing this (or able to do this) please contribute    
the code or your wisdom here. jbrohan@gmail.com    
2) The  Security.addProvider() is not used. Not sure what effect this has,    
but again if you know how to solve it...chip in!   
3) My impression is that it will work for any valid gmail account. I made    
a new one, and it worked just fine. Will it work on an Android phone without    
a gmail account?   
4) All the relevant files are in the listing. You need Permissions for INTERNET.   
/* good luck to all venturing into these stormy waters */  