package com.libreria.auth.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
public class PasswordReset {
 @Id public String tokenHash;
 @ManyToOne(optional=false) public Usuario usuario;
 @Column(nullable=false) public Instant expiresAt;
 public boolean used;
 public PasswordReset(){}
}
