import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivateAccountService } from '../services/activate-account.service';

@Component({
  selector: 'app-activate-account',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './activate-account.component.html',
  styleUrls: ['./activate-account.component.scss'],
})
export class ActivateAccountComponent implements OnInit {
  token: string | null = null;
  loading = false;
  isSubmitting = false;
  error: string | null = null;
  success = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private activateService: ActivateAccountService,
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token');
    console.log('Token from URL:', this.token);
    if (!this.token) {
      this.error = 'Invalid activation link.';
    }
  }

  onSubmit(): void {
    console.log('onSubmit called, token:', this.token);
    if (!this.token) {
      this.error = 'Invalid activation link.';
      return;
    }

    this.isSubmitting = true;
    this.error = null;

    this.activateService.activate(this.token).subscribe({
      next: () => {
        console.log('Activation successful');
        this.success = true;
        this.isSubmitting = false;
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (err) => {
        console.error('Activation error:', err);
        this.error = 'Account activation failed. Please try again or contact support.';
        this.isSubmitting = false;
      },
    });
  }
}
