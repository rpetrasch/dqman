import { TestBed } from '@angular/core/testing';

import { DqProject } from './dq-project';

describe('DqProject', () => {
  let service: DqProject;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DqProject);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
