Libbie 
Repo for Project 1 at Malmö University (MAU)  
[GitHub Repository](https://github.com/masalkovski3/Libbie.git)

Innehåll:
Ett Java Spring Boot-projekt som hanterar medlemmar och bokhyllor. Den innehåller backend-logik för att hantera användare, profiler, inloggning, och böcker.

Förutsättningar

Innan du börjar, se till att följande är installerat:

- [Java JDK 17+](https://adoptopenjdk.net/)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- [Maven](https://maven.apache.org/) (om inte integrerat i IntelliJ)
- [Git](https://git-scm.com/)

Klona projektet

```bash
git clone https://github.com/masalkovski3/Libbie.git
cd Libbie Öppna i IntelliJ IDEA

Öppna IntelliJ
Välj "Open" och navigera till den nedladdade mappen Libbie.
IntelliJ kommer automatiskt att importera projektet som ett Maven-projekt.
Skapa Member-klassen
Navigera till src/main/java/com/openlibrary/demo/model/ och skapa filen:

Memeber.java:
package com.openlibrary.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    // Valfria fält, kan byggas på
    private String name;

    private String bio;

    private String profileImage;

    // Constructors
    public Member() {
    }

    public Member(String username, String password, String name) {
        this.username = username;
        this.password = password;
        this.name = name;
    }

    // Getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String fullName) { this.name = fullName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
}

Kör applikationen
Högerklicka på huvudklassen (DemoApplication.java eller liknande).
Välj Run.

Testa i webbläsaren
Öppna din webbläsare och gå till:

http://localhost:8080
Du bör se startsidan för applikationen om allt fungerar korrekt.


Tips

Om något inte fungerar, kontrollera application.properties eller application.yml i src/main/resources/.
Använd Postman eller curl för att testa API-endpoints.


Utveckling

Projektet utvecklas som del av kursen i systemutveckling vid MAU.
Team: Grupp 21
