# PlaqueAnalyzer Studio

PlaqueAnalyzer Studio is a web application designed to visualize redundancies (referred to as **plaque**) based on relational information content, while providing normalization to reduce them.  
The app is implemented with **Spring Boot** and **Maven**, and relies on the [`relational_information_content`](https://github.com/sdbs-uni-p/relational_information_content) tool (included as an external JAR in `libs/`).

<img width="1251" height="1661" alt="projectss1" src="https://github.com/user-attachments/assets/3a323001-f2d7-4e05-80a6-8724eca2673e" /> 
<img width="1233" height="980" alt="projectss2" src="https://github.com/user-attachments/assets/ba61d576-6789-4ddb-8536-600925107fc9" />

## Build and Run Options

Please choose one method:

- **A) Local (without Docker)**
- **B) Docker**

## A) Run Locally (without Docker)

## Prerequisites

- **Java (JDK 17 or the version required by this project)**
- **Maven**
- **PostgreSQL & pgAdmin (running locally)**

## Steps

1. **Clone the repository**

   ```bash
   git clone https://github.com/Atakan97/PlaqueAnalyzerStudio.git
   cd PlaqueAnalyzerStudio
   ```

2. **Configure database credentials (PostgreSQL)**

   Install PostgreSQL, then create the database and user:

   ```sql
   CREATE DATABASE plaque_db;
   CREATE USER plaque_user WITH PASSWORD 'user123';
   GRANT ALL PRIVILEGES ON DATABASE plaque_db TO plaque_user;
   ```

   If you use different credentials, please update `application.properties`:

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/plaque_db
   spring.datasource.username=plaque_user
   spring.datasource.password=user123
   ```

3. **Build the project**

   ```bash
   mvn clean package
   ```

4. **Run the application**

   ```bash
   mvn spring-boot:run
   ```

5. **Open the application**

   http://localhost:8080

## B) Run with Docker

## Prerequisites

- **Docker Desktop**

## Steps

1. **Clone the repository**

   ```bash
   git clone https://github.com/Atakan97/PlaqueAnalyzerStudio.git
   cd PlaqueAnalyzerStudio
   ```

2. **Start all services**

   ```bash
   docker compose up --build
   ```

3. **Open the application**

   http://localhost:8080

---

## Application Admin Panel Credentials (Applies to Both Local and Docker Runs)

After the application starts (using either **A) Local** or **B) Docker**), you can log in to the admin panel with (not the database):

- **Username:** `admin`
- **Password:** `1234`

Admin credentials are configured in:

```text
src/main/resources/application.properties
```

## About

This project was conducted under the supervision of Stefanie Scherzinger and Christoph Köhnen as part of the Master’s thesis of Atakan Arda Celik.

The application is based on the ideas introduced in this paper:  
**A Plaque Test for Redundancies in Relational Data** by Christoph Köhnen, Stefan Klessinger, Jens Zumbrägel and Stefanie Scherzinger, published in the QDB workshop co-located with VLDB 2023.


If you use this repository, please cite **PlaqueAnalyzer Studio**:

```bibtex
@misc{celik2026plaqueanalyzerstudio,
  author       = {Atakan Arda Celik and
                  Christoph K{\"{o}}hnen and 
                  Stefanie Scherzinger
  },
  title        = {PlaqueAnalyzer Studio},
  note         = {Master's thesis software project, \url{https://github.com/Atakan97/PlaqueAnalyzerStudio}},
  year         = {2026}
}
```

## License

This project is licensed under the GNU General Public License v3.0 (GPL-3.0).