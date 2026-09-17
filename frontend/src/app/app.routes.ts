import { Routes } from '@angular/router';
import { ChallengeActivityComponent } from './challenge-activity/challenge-activity.component';
import { ChallengeCreateComponent } from './challenge-create/challenge-create.component';
import { ChallengeDetailComponent } from './challenge-detail/challenge-detail.component';
import { EngineDemoComponent } from './engine-demo.component';

export const routes: Routes = [
  { path: '', component: EngineDemoComponent },
  { path: 'actividad', component: ChallengeActivityComponent },
  { path: 'desafios/nuevo', component: ChallengeCreateComponent },
  { path: 'desafios/:id', component: ChallengeDetailComponent },
  { path: '**', redirectTo: '' },
];
