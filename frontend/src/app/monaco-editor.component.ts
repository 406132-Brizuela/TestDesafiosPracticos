import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';

declare global {
  interface Window {
    monaco?: any;
    require?: any;
    MonacoEnvironment?: any;
  }
}

const MONACO_BASE_PATH = 'monaco/vs';

let monacoLoaderPromise: Promise<any> | null = null;

/**
 * Carga el AMD build de Monaco (node_modules/monaco-editor/min/vs, copiado como asset a
 * /monaco/vs) mediante su loader.js clasico, sin pasar por el bundler de Angular. Es el
 * enfoque mas robusto para integrar Monaco independientemente del bundler del host.
 */
function loadMonaco(): Promise<any> {
  if (monacoLoaderPromise) {
    return monacoLoaderPromise;
  }

  monacoLoaderPromise = new Promise((resolve, reject) => {
    if (window.monaco) {
      resolve(window.monaco);
      return;
    }

    const script = document.createElement('script');
    script.src = `${MONACO_BASE_PATH}/loader.js`;
    script.onload = () => {
      const origin = window.location.origin;

      window.MonacoEnvironment = {
        getWorkerUrl: () => {
          const bootstrap = `
            self.MonacoEnvironment = { baseUrl: '${origin}/monaco/' };
            importScripts('${origin}/${MONACO_BASE_PATH}/loader.js');
            self.require.config({ paths: { vs: '${origin}/${MONACO_BASE_PATH}' } });
            self.require(['vs/editor/editor.worker'], function () {});
          `;
          const blob = new Blob([bootstrap], { type: 'application/javascript' });
          return URL.createObjectURL(blob);
        },
      };

      window.require!.config({ paths: { vs: MONACO_BASE_PATH } });
      window.require!(['vs/editor/editor.main'], () => resolve(window.monaco));
    };
    script.onerror = reject;
    document.body.appendChild(script);
  });

  return monacoLoaderPromise;
}

@Component({
  selector: 'app-monaco-editor',
  standalone: true,
  template: `<div #host class="monaco-host"></div>`,
  styleUrl: './monaco-editor.component.css',
})
export class MonacoEditorComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() value = '';
  @Input() language = 'java';
  @Output() valueChange = new EventEmitter<string>();

  @ViewChild('host', { static: true }) hostRef!: ElementRef<HTMLDivElement>;

  private editor: any;
  private ready = false;

  ngAfterViewInit(): void {
    loadMonaco().then((monaco) => {
      monaco.editor.defineTheme('desafios-dark', {
        base: 'vs-dark',
        inherit: true,
        rules: [],
        colors: {
          'editor.background': '#0d1220',
          'editor.foreground': '#d7dceb',
          'editorLineNumber.foreground': '#3a4358',
          'editorLineNumber.activeForeground': '#8b93a7',
          'editor.selectionBackground': '#6366f140',
          'editorCursor.foreground': '#818cf8',
          'editor.lineHighlightBackground': '#161f34',
          'editorIndentGuide.background': '#1a2236',
          'editorIndentGuide.activeBackground': '#2a3450',
        },
      });

      this.editor = monaco.editor.create(this.hostRef.nativeElement, {
        value: this.value,
        language: this.language,
        theme: 'desafios-dark',
        automaticLayout: true,
        minimap: { enabled: false },
        fontFamily: "'JetBrains Mono', 'Fira Code', Consolas, monospace",
        fontSize: 14,
        fontLigatures: true,
        lineHeight: 22,
        scrollBeyondLastLine: false,
        renderLineHighlight: 'gutter',
        tabSize: 4,
        padding: { top: 16, bottom: 16 },
        scrollbar: { verticalScrollbarSize: 10, horizontalScrollbarSize: 10 },
      });

      this.ready = true;

      this.editor.onDidChangeModelContent(() => {
        this.valueChange.emit(this.editor.getValue());
      });
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['value'] && this.ready && this.editor) {
      const current = this.editor.getValue();
      if (current !== this.value) {
        this.editor.setValue(this.value);
      }
    }
  }

  ngOnDestroy(): void {
    this.editor?.dispose();
  }
}
