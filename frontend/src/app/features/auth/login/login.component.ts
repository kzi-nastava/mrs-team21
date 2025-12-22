import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  email: string = '';
  password: string = '';
  rememberMe: boolean = false;

  onSubmit(): void {
    // TODO: Implement login logic in KT2
    console.log('Login attempt:', { email: this.email, rememberMe: this.rememberMe });
  }
}
