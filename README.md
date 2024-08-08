
# Delta Review with AI Plugin

## Overview
The Delta Review with AI Plugin is an IntelliJ IDEA plugin designed to assist in code review processes using various AI models such as Claude, ChatGPT, Gemini, and more. This plugin enables users to review code changes automatically, identify potential risks, and suggest improvements based on AI-generated feedback.

## Features
- **Automatic Code Review**: Automatically review code changes and provide AI-generated feedback.
- **Customizable AI Settings**: Choose from multiple AI service providers and configure settings such as API endpoint, model, and response paths.
- **Language Support**: Dynamically choose the preferred language for the AI response.
- **Detailed Feedback**: Receive detailed feedback on code changes, potential risks, and suggestions for improvement.

## Installation
1. Clone the repository:
   ```bash
   git clone <repository-url>
   ```
2. Open the project in IntelliJ IDEA.

3. Build and run the plugin using the Gradle task:
   ```bash
   ./gradlew runIde
   ```

## Usage
1. **Configure Settings**: Go to the plugin settings to configure your API key, service provider, model, and other settings.
2. **Review Changed Files**: The plugin will automatically detect changed files in your project and send them to the AI for review.
3. **Receive Feedback**: View the feedback directly within the IDE.

## Configuration
The plugin allows customization of the following settings:
- **Endpoint**: API endpoint for the selected service provider.
- **Max Tokens**: Maximum number of tokens for the AI response.
- **File Extensions**: Regular expression to filter which files should be sent for review.
- **Model**: The AI model to use for generating feedback.
- **API Key**: Your API key for the selected service provider.
- **Preferred Language**: The language in which you want to receive the feedback.

## Example Code
```kotlin
val settings = DeltaReviewSettings.instance
val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .addInterceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        println("HTTP ${request.method} ${request.url}")
        println("Request headers: ${request.headers}")
        println("Response code: ${response.code}")
        println("Response headers: ${response.headers}")
        response
    }
    .build()
```

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributions
Contributions are welcome! Please fork this repository, create a new branch, and submit a pull request.

## Contact
For any issues or feature requests, please open an issue on GitHub or contact the repository maintainer.
