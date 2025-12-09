import { TestBed } from '@angular/core/testing';

import { DqProjectService } from './dq-project.service';

describe('DqProjectService', () => {
  let service: DqProjectService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DqProjectService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
