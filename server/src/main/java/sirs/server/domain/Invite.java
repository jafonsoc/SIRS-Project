package sirs.server.domain;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Arrays;

@Entity
@Table(name = "invite")
public class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    private File file;

    @Column(name = "fileKey", nullable = false, columnDefinition = "BLOB")
    private byte[] fileKey;

    /*
        I, João Coutinho ist189470, will buy a burguer for whomever finds a way
        to remove invites from the DB when they are accepted. Until then, this
        horrible boolean will live here as a testimony to my past suffering.
    */
    @Column(name = "accepted", nullable = false)
    private boolean accepted;

    public Invite() {
    }

    public Invite(User user, File file, byte[] fileKey) {
        this.user = user;
        this.file = file;
        this.fileKey = fileKey;
        this.accepted = false;
    }

    public boolean getAccepted() {
        return this.accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getId() {
        return id;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public byte[] getFileKey() {
        return fileKey;
    }

    public void setFileKey(byte[] fileKey) {
        this.fileKey = fileKey;
    }

    @Override
    public String toString() {
        return "Invite{" +
                "id=" + id +
                ", file=" + file +
                ", fileKey=" + Arrays.toString(fileKey) +
                '}';
    }
}
