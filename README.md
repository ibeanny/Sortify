# Sortify

Sortify is a full-stack application that transforms unstructured text files into organized, categorized data using AI.

Upload a messy `.txt` file, and Sortify analyzes and structures the content into readable sections.

## Features

- Upload `.txt` files
- AI-powered categorization of unstructured data
- Generates structured and readable results
- Clean output formatting
- Real-time processing through a full-stack system

## How It Works

1. The user uploads a text file through the React frontend
2. The file is sent to a Spring Boot backend via an API
3. The backend processes the file line-by-line
4. Each line is analyzed and categorized using OpenAI
5. The results are grouped into structured sections
6. The frontend displays the organized output

## Tech Stack

Frontend:
- React
- Vite
- CSS

Backend:
- Java
- Spring Boot
- REST API

AI Integration:
- OpenAI API

## Setup

### Clone the repository

git clone https://github.com/ibeanny/sortify.git
cd sortify

### Configure API Key

Create a file:

src/main/resources/application.properties

Add:

openai.api.key=YOUR_API_KEY_HERE

### Run Backend

Run the Spring Boot application in IntelliJ  
or run:

./gradlew bootRun

### Run Frontend

cd frontend  
npm install  
npm run dev

## Future Improvements

- Enhance UI/UX design
- Add drag-and-drop file upload
- Support additional file formats
- Add user authentication

## Author

Elvis Ortiz  
Computer Science Student :D