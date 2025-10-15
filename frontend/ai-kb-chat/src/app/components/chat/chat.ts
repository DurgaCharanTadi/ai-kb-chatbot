import { Component, signal } from '@angular/core';
import { NgClass, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { HttpClient, HttpClientModule } from '@angular/common/http';

interface ChatMessage {
  from: 'user' | 'assistant';
  text: string;
  timestamp: Date;
}

type ResizeMode = 'h' | 'v' | null;

// API response shape (match your BedrockKbService DTO)
interface RagResponse {
   answer: string;
   citations?: Array<{
     snippetFromAnswer: string;
     references: Array<{ title: string; source: string }>;
   }>;
   sessionId?: string;
 }

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [
    NgClass,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    DatePipe,
    HttpClientModule 
  ],
  templateUrl: './chat.html',
  styleUrls: ['./chat.css']
})
export class Chat {
  // Panel state (closed by default)
  isOpen = signal(false);

  // Dimensions as signals (smooth updates in zoneless mode)
  width = signal(360);
  height = signal(520);

  // Compose box
  newMessage: string = '';

  // Fake chat messages
  messages = signal<ChatMessage[]>([
    { from: 'assistant', text: 'Hi there! How can I help you today?', timestamp: new Date() }
  ]);

  // backend URL (use your Spring Boot endpoint)
  private apiUrl = 'https://bb42728jqa.execute-api.us-east-1.amazonaws.com/dev/api/rag';
  // optional session id to keep context on the server
  private sessionId = `web-${crypto.randomUUID().slice(0, 8)}`;
  // track loading state
  loading = signal(false);

// inject HttpClient
constructor(private http: HttpClient) {}

  // Toggle open/close
  toggle(): void {
    this.isOpen.update(o => !o);
  }

  // Send a message and simulate a reply
  send(): void {
    const trimmed = this.newMessage.trim();
    if (!trimmed) return;

    const userMsg: ChatMessage = { from: 'user', text: trimmed, timestamp: new Date() };
    this.messages.update(list => [...list, userMsg]);
    this.newMessage = '';

   // optimistic "typing…" bubble
  const typingMsg: ChatMessage = {
   from: 'assistant',
   text: '…thinking',
   timestamp: new Date()
  };
  this.messages.update(list => [...list, typingMsg]);
  this.loading.set(true);

 // call Spring API
  const body = {
    question: trimmed,
    sessionId: this.sessionId
    // knowledgeBaseId / modelArn not required if defaults set in application.yml
  };

  this.http.post<RagResponse>(this.apiUrl, body).subscribe({
    next: (res) => {
      // Build display text with optional citations
      let answer = res?.answer ?? '(no answer)';
      if (res?.citations?.length) {
        const flatRefs = res.citations
           .flatMap(c => c.references || [])
           .filter(r => r?.source)
           .slice(0, 5) // limit to a few sources
           .map((r, i) => `[\u{1F517}${i + 1}] ${r.title || 'source'} — ${r.source}`)
           .join('\n');
         if (flatRefs) {
           answer += `\n\nSources:\n${flatRefs}`;
         }
       }

       // Replace the "typing…" bubble with real answer
       this.messages.update(list => {
         const updated = [...list];
         updated[updated.length - 1] = { from: 'assistant', text: answer, timestamp: new Date() };
         return updated;
       });
       this.loading.set(false);
     },
     error: (err) => {
       const msg = (err?.error?.message || err?.message || 'Request failed').toString();
       this.messages.update(list => {
         const updated = [...list];
         updated[updated.length - 1] = {
           from: 'assistant',
           text: `Sorry, I couldn’t complete that request.\n\nDetails: ${msg}`,
           timestamp: new Date()
         };
         return updated;
       });
       this.loading.set(false);
     }
   });
  }

  // ----- Resizing (separate handles: left for width, top for height) -----
  private resizing: ResizeMode = null;
  private startX = 0;
  private startY = 0;
  private startW = 0;
  private startH = 0;

  // rAF batching
  private rafPending = false;
  private pendingW = 0;
  private pendingH = 0;

  onResizeStartLeft(event: MouseEvent) {
    event.preventDefault();
    this.resizing = 'h';
    this.startX = event.clientX;
    this.startW = this.width();

    document.body.style.cursor = 'ew-resize';
    (document.body.style as any).userSelect = 'none';

    window.addEventListener('mousemove', this.onResizing, { passive: true });
    window.addEventListener('mouseup', this.onResizeEnd, { passive: true });
  }

  onResizeStartTop(event: MouseEvent) {
    event.preventDefault();
    this.resizing = 'v';
    this.startY = event.clientY;
    this.startH = this.height();

    document.body.style.cursor = 'ns-resize';
    (document.body.style as any).userSelect = 'none';

    window.addEventListener('mousemove', this.onResizing, { passive: true });
    window.addEventListener('mouseup', this.onResizeEnd, { passive: true });
  }

  private onResizing = (e: MouseEvent) => {
    if (!this.resizing) return;

    const maxW = Math.round(window.innerWidth * 0.9);
    const maxH = Math.round(window.innerHeight * 0.9);

    if (this.resizing === 'h') {
      // Horizontal: dragging LEFT increases width (panel anchored to right)
      const dx = e.clientX - this.startX;
      this.pendingW = Math.min(Math.max(this.startW - dx, 320), maxW);
      if (!this.rafPending) {
        this.rafPending = true;
        requestAnimationFrame(() => {
          this.width.set(this.pendingW);
          this.rafPending = false;
        });
      }
    } else if (this.resizing === 'v') {
      // Vertical: dragging UP increases height (panel anchored to bottom)
      const dy = e.clientY - this.startY;
      this.pendingH = Math.min(Math.max(this.startH - dy, 360), maxH);
      if (!this.rafPending) {
        this.rafPending = true;
        requestAnimationFrame(() => {
          this.height.set(this.pendingH);
          this.rafPending = false;
        });
      }
    }
  };

  private onResizeEnd = () => {
    if (!this.resizing) return;
    this.resizing = null;

    document.body.style.cursor = '';
    (document.body.style as any).userSelect = '';

    window.removeEventListener('mousemove', this.onResizing);
    window.removeEventListener('mouseup', this.onResizeEnd);
  };
}
