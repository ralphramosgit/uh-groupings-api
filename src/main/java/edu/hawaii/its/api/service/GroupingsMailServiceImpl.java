package edu.hawaii.its.api.service;

import com.opencsv.CSVWriter;

import edu.hawaii.its.api.type.GroupingsServiceResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Create and send SMTP messages using the JavaMailSender.
 */
@Service("groupingsMailService")
public class GroupingsMailServiceImpl implements GroupingsMailService {
    @Autowired
    JavaMailSender javaMailSender;

    public void setJavaMailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /**
     * Send a SMTP message with no attachment
     *
     * @param address - email address to be sent to
     * @param subject - email subject
     * @param text    - email text body
     */
    @Override public void sendSimpleMessage(String address, String subject, String text) {

        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setTo(address);
        simpleMailMessage.setSubject(subject);
        simpleMailMessage.setText(text);
        this.javaMailSender.send(simpleMailMessage);

    }

    /**
     * Send an SMTP message with a CSV file attachment.
     *
     * @param address - email address to be sent to
     * @param subject - email subject
     * @param text    - email text body
     * @param path    - path or name of the temporary CSV file
     * @param res     - data to be converted to CSV
     */
    @Override public void sendCSVMessage(String address, String subject, String text, String path,
            List<GroupingsServiceResult> res) {

        File file = new File(path);

        try {
            this.sendAttachmentMessage(address, subject, text, this.toCsv(this.toCsvObj(res), file), file);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Send an SMTP message with a file attachment
     *
     * @param address            - email address to be sent to
     * @param subject            - email subject
     * @param text               - email text body
     * @param fileSystemResource - File being sent
     * @param file               - descriptor of fil being sent
     */
    private void sendAttachmentMessage(String address, String subject, String text,
            FileSystemResource fileSystemResource, File file) {

        try {
            MimeMessage mimeMessage = this.javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            mimeMessageHelper.setFrom(getUhSmtpAddress());
            mimeMessageHelper.setTo(address);
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setText(text);

            mimeMessageHelper.addAttachment(file.getPath(), fileSystemResource);
            javaMailSender.send(mimeMessage);
            file.delete();

        } catch (MessagingException me) {
            me.printStackTrace();
        }

    }

    /**
     * Build the lines of a CSV file
     *
     * @param res - data which csv is being build from
     * @return - a list of String array, where each String array is a line in the CSV
     */
    private List<String[]> toCsvObj(List<GroupingsServiceResult> res) {
        List<String[]> lines = new ArrayList<>();

        lines.add(new String[] { "username", "uuid", "firstName", "lastName", "name" });

        for (GroupingsServiceResult item : res) {
            lines.add(item.getPerson().toCsv());
        }
        return lines;
    }

    /**
     * Write Csv data to a file.
     *
     * @param data - Csv data returned from toCsvObj()
     * @param file - Descriptor ro be written too.
     * @return - FileSystemResource to be sent via SMTP.
     * @throws IOException
     */
    private FileSystemResource toCsv(List<String[]> data, File file) throws IOException {
        try {
            FileWriter out = new FileWriter(file);
            CSVWriter writer = new CSVWriter(out);

            writer.writeAll(data);
            writer.close();

        } catch (FileNotFoundException e) {
            throw new IOException(e);
        }

        return new FileSystemResource(file);
    }

    /* Concat UH suffix onto username */
    public String getUserEmail(String username) {
        return username + "@hawaii.edu";
    }

    /* Get from address */
    private String getUhSmtpAddress() {
        return "smtp.hawaii.edu";
    }
}
