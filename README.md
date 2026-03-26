# Sortify

Sortify is a full-stack application that transforms unstructured text files into organized, categorized data using AI.

Upload one or more messy `.txt` files, and Sortify analyzes and structures the content into readable sections.

## Features

- Upload one or more `.txt` files
- Drag and drop `.txt` files into the web app
- AI-powered categorization of unstructured data
- Generates structured and readable results per file
- Builds one combined result across all uploaded files
- Download one sorted `.txt` per uploaded file or one combined export
- Clean output formatting
- Expand or collapse each file result and the combined result
- Real-time processing through a full-stack system

## How It Works

1. The user uploads or drags in one or more `.txt` files through the React frontend
2. The files are sent to a Spring Boot backend via an API
3. The backend processes each file line-by-line
4. Each line is analyzed and categorized using OpenAI
5. The results are grouped into structured sections for each file
6. The frontend also builds a combined grouped view across all uploaded files
7. The user can expand, collapse, and download the organized output

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

Set an environment variable:

PowerShell:

`setx OPENAI_API_KEY "YOUR_API_KEY_HERE"`

Then restart your terminal or IDE.

Create a file:

src/main/resources/application.properties

Add:

spring.application.name=aisorter
openai.api.key=${OPENAI_API_KEY}

### Run Backend

Run the Spring Boot application in IntelliJ  
or run:

./gradlew bootRun

### Run Frontend

cd frontend  
npm install  
npm run dev

Open the Vite app in your browser and:

- choose multiple `.txt` files from the picker, or
- drag and drop multiple `.txt` files into the upload panel

After processing, Sortify shows:

- one collapsible result box per file
- one final combined result box for all uploaded files
- a button to download one sorted `.txt` per uploaded file
- a button to download one combined `.txt`

## Future Improvements

- Enhance UI/UX design
- Support additional file formats
- Add user authentication

## Author

Elvis Ortiz  
Computer Science Student :D
