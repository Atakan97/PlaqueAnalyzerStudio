# PlaqueAnalyzer Studio

PlaqueAnalyzer Studio is a full-stack web application designed to visualize redundancies (referred to as **plaque**) based on relational information content, while providing normalization to reduce them.  
The app is implemented with **Spring Boot** and **Maven**, and relies on the  
[`relational_information_content`](https://github.com/sdbs-uni-p/relational_information_content) tool (included as an external JAR in `libs/`).

## About

This project was conducted under the supervision of Stefanie Scherzinger and Christoph Köhnen as part of the Master’s thesis of Atakan Arda Celik.  
The application is based on the ideas introduced in this paper:  
**“A Plaque Test for Redundancies in Relational Data.”** 


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

Cite the underlyting paper:

```bibtex
@inproceedings{DBLP:conf/vldb/KohnenKZS23,
  author       = {Christoph K{\"{o}}hnen and
                  Stefan Klessinger and
                  Jens Zumbr{\"{a}}gel and
                  Stefanie Scherzinger},
  title        = {A Plaque Test for Redundancies in Relational Data},
  booktitle    = {Joint Proceedings of Workshops at the 49th International Conference
                  on Very Large Data Bases {(VLDB} 2023), Vancouver, Canada, August
                  28 - September 1, 2023},
  series       = {{CEUR} Workshop Proceedings},
  volume       = {3462},
  year         = {2023}
}
```

## License

This project is licensed under the GNU General Public License v3.0 (GPL-3.0).

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
   git clone https://git.fim.uni-passau.de/sdbs/theses/students/mt-atakan-celik-code.git
   cd mt-atakan-celik-code
   ```

2. **Configure database credentials**

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
   git clone https://git.fim.uni-passau.de/sdbs/theses/students/mt-atakan-celik-code.git
   cd mt-atakan-celik-code
   ```

2. **Start all services**

   ```bash
   docker compose up --build
   ```

3. **Open the application**

   http://localhost:8080

## Admin Panel Access

You can log in to the admin panel with the following default credentials:

- **Username:** `admin`
- **Password:** `1234`

Admin credentials are configured in:

```text
src/main/resources/application.properties
```