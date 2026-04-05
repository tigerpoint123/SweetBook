declare module "page-flip" {
  export interface PageFlipEvent {
    data: number | string | boolean | object | null;
    object: PageFlip;
  }

  export interface PageFlipSettings {
    width: number;
    height: number;
    size?: "fixed" | "stretch";
    minWidth?: number;
    maxWidth?: number;
    minHeight?: number;
    maxHeight?: number;
    showCover?: boolean;
    maxShadowOpacity?: number;
    flippingTime?: number;
    mobileScrollSupport?: boolean;
    usePortrait?: boolean;
    startPage?: number;
    autoSize?: boolean;
    drawShadow?: boolean;
    useMouseEvents?: boolean;
    showPageCorners?: boolean;
  }

  export class PageFlip {
    constructor(element: HTMLElement, settings: PageFlipSettings);
    loadFromImages(urls: string[]): void;
    destroy(): void;
    off(event: string): void;
    flipNext(corner?: string): void;
    flipPrev(corner?: string): void;
    turnToPage(page: number): void;
    getCurrentPageIndex(): number;
    on(event: string, callback: (e: PageFlipEvent) => void): PageFlip;
  }
}
