import { Component, signal } from '@angular/core';
import { Chat } from "./components/chat/chat";
import { MatIconModule } from "@angular/material/icon";

@Component({
  selector: 'app-root',
  imports: [Chat, MatIconModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  title = signal('My Angular‑20 Application');
  description = signal('This application showcases a landing page with a chat overlay.  It will soon integrate with a Spring Boot backend to answer questions using AWS Bedrock, Athena/Glue and SageMaker.');
  usageInstructions = signal<string[]>([
    'Explore the landing page to learn about the app.',
    'Click the chat icon in the bottom right to open the chat.',
    'Type your question and press Enter or click Send.',
    'A fake response will appear after a short delay.'
  ]);
}
