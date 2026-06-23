/// <reference types="vite/client" />

declare namespace React {
  namespace JSX {
    interface IntrinsicElements {
      'model-viewer': React.DetailedHTMLProps<React.HTMLAttributes<HTMLElement>, HTMLElement> & {
        src?: string;
        'auto-rotate'?: boolean | string;
        'camera-controls'?: boolean | string;
        'shadow-intensity'?: string | number;
        'environment-image'?: string;
        'exposure'?: string | number;
        alt?: string;
        poster?: string;
      };
    }
  }
}
