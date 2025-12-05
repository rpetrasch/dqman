import { TestBed } from '@angular/core/testing';

import { DqRuleService } from './dq-rule.service';

describe('DqRuleService', () => {
  let service: DqRuleService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DqRuleService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
