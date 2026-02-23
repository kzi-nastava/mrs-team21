import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';

/**
 * Placeholder for features required by spec but not yet implemented.
 * Route data: featureName (string), specRef (string, optional).
 * See docs/navbar-removed-features-spec.md.
 */
@Component({
  selector: 'app-placeholder-feature',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './placeholder-feature.component.html',
  styleUrl: './placeholder-feature.component.scss',
})
export class PlaceholderFeatureComponent {
  private readonly route = inject(ActivatedRoute);

  readonly featureName = this.route.snapshot.data['featureName'] as string ?? 'This feature';
  readonly specRef = this.route.snapshot.data['specRef'] as string | undefined;
}
