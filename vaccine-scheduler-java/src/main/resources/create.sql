CREATE TABLE Caregivers (
    Username VARCHAR(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time DATE,
    Username VARCHAR(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Patients (
    Username VARCHAR(255),
    Salt     BINARY(16),
    Hash     BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Vaccines (
    Name VARCHAR(255),
    Doses INT,
    PRIMARY KEY (Name)
);


CREATE TABLE Appointments (
    ID INT,
    Time DATE,
    Patient_Username VARCHAR(255) REFERENCES Patients(Username),
    Vaccine_Name VARCHAR(255) REFERENCES Vaccines(Name),
    Caregiver_Username VARCHAR(255) REFERENCES Caregivers(Username),
    PRIMARY KEY (ID)
);