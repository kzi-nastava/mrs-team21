import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { RideEstimatePanelComponent } from '../components/ride-estimate-panel/ride-estimate-panel.component';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [RouterLink, RideEstimatePanelComponent],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.scss',
})
export class LandingPageComponent {
  isPanelOpen = signal(false);

  openPanel(): void {
    this.isPanelOpen.set(true);
  }

  closePanel(): void {
    this.isPanelOpen.set(false);
  }
}
