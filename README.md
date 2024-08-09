
# On-device AI Code Review Plugin

## Overview
The **On-device AI Code Review Plugin** is a powerful tool designed to automate code reviews within IntelliJ-based IDEs. It leverages various AI models (e.g., Claude, ChatGPT, Gemini, etc.) to analyze code changes, identify potential risks, and suggest improvements, making your code review process more efficient and productive.

## Key Features
- **Automatic Code Review**: Automatically review changed files and provide AI-generated feedback.
- **Support for Multiple AI Models**: Choose from multiple AI service providers and configure settings like API endpoint, model, and response paths.
- **Multi-Language Support**: Dynamically select the preferred language for AI responses.
- **Detailed Feedback**: Receive comprehensive feedback on code changes, potential risks, and suggestions for improvement.
- **Customizable Settings**: Tailor the plugin’s settings, including endpoint, tokens, file extensions, model, and more, to fit your workflow.

## Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/yhzion/on-device-ai-code-review-jetbrains-plugin.git
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

---

This plugin helps automate code reviews using AI in JetBrains IDEs. It supports multiple code review providers and improves developers' workflows by analyzing changes to code and providing detailed review results.
