declare module "page-flip" {
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
    flipNext(corner?: string): void;
    flipPrev(corner?: string): void;
    turnToPage(page: number): void;
    getCurrentPageIndex(): number;
    on(event: string, callback: (e: unknown) => void): PageFlip;
  }
}
