import { Routes } from '@angular/router';
import { AttemptResolveComponent } from './attempt-resolve/attempt-resolve.component';
import { ChallengeActivityComponent } from './challenge-activity/challenge-activity.component';
import { ChallengeCreateComponent } from './challenge-create/challenge-create.component';
import { ChallengeDetailComponent } from './challenge-detail/challenge-detail.component';
import { EngineDemoComponent } from './engine-demo.component';

export const routes: Routes = [
  { path: '', component: EngineDemoComponent },
  { path: 'actividad', component: ChallengeActivityComponent },
  { path: 'desafios/nuevo', component: ChallengeCreateComponent },
  { path: 'desafios/:id', component: ChallengeDetailComponent },
  { path: 'intentos/:id', component: AttemptResolveComponent },
  { path: '**', redirectTo: '' },
];
