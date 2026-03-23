# MT-Atakan-Celik-Code

## PlaqueAnalyzer Studio

PlaqueAnalyzer Studio is a full-stack web application designed to visualize redundancies (referred to as `plaque`) based on relational information content, while providing normalization to reduce them.  
The app is implemented with **Spring Boot** and **Maven**, and relies on the  
[`relational_information_content`](https://github.com/sdbs-uni-p/relational_information_content) tool (included as an external JAR in `libs/`).

## About

This project was conducted under the supervision of Stefanie Scherzinger and Christoph Köhnen as part of the Master's thesis of Atakan Arda Celik.
The application is built upon the concepts and research presented in the paper "A Plaque Test for Redundancies in Relational Data". 

## License

This project is licensed under the GNU General Public License v3.0 (GPL-3.0).

## Prerequisites

- **Java (JDK 17 or the version required by this project)** 
- **Maven**  
- **PostgreSQL & pgAdmin (running locally)**

## Project Setup

1. **Clone the repository**

   ```bash
   git clone https://git.fim.uni-passau.de/sdbs/theses/students/mt-atakan-celik-code.git
   cd mt-atakan-celik-code
   ```

2. **Verify the external RIC JAR** 

   This project depends on the following external JAR file, which should already be included in `libs/`:

   ```text
   libs/relational_information_content-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

   > This file is required for the application to start correctly.

3. **(Optional) Rebuild the RIC JAR if it is missing**

   If the file above is not present, build it from the `relational_information_content` project and copy it into this project's `libs/ ` directory.

   ```bash
   cd path/to/relational_information_content
   mvn -DskipTests package
   ```

   Then copy:

   ```text
   target/relational_information_content-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

   to:

   ```text
   mt-atakan-celik-code/libs/
   ```

4. **Configure `application.properties`**

   Check the configuration file at:

   ```text
   src/main/resources/application.properties
   ```

   Make sure the RIC JAR path is configured as:

   ```properties
   ric.jar.path=libs/relational_information_content-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

   If you use a different path, update this property accordingly.

5. **Set up PostgreSQL**

  Install PostgreSQL, then create the database and user:

   ```sql
   CREATE DATABASE plaque_db;
   CREATE USER plaque_user WITH PASSWORD 'user123';
   GRANT ALL PRIVILEGES ON DATABASE plaque_db TO plaque_user;
   ```

   If you use different credentials, update `application.properties`:

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/plaque_db
   spring.datasource.username=plaque_user
   spring.datasource.password=user123
   ```

## Running Project

1. **Build the project**

   Use the following command to build the project:

   ```bash
   mvn clean package
   ```

2. **Run the application**

   Start the application with Maven:

   ```bash
   mvn spring-boot:run
   ```

3. **Access the application**

   Once the application is running, open:

   http://localhost:8080

## Running the Project with Docker

1. **Open a terminal in the project folder and run Docker Compose**  
   *(_Docker Desktop must be installed._)*

   After navigating to the project directory, run:

   ```bash
   docker compose up --build
   ```

   This command builds the project image, pulls the required PostgreSQL image, and starts both the application and the database.

   The first run may take a few minutes, depending on your internet speed.