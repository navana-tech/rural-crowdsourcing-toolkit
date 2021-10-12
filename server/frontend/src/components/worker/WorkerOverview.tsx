// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

/**
 * Component to display a graph with the required workers' data
 */

// React stuff
import * as React from 'react';

// Redux stuff
import { connect, ConnectedProps } from 'react-redux';
import { compose } from 'redux';
import { RootState } from '../../store/Index';

// Store types and actions
import { WorkerRecord } from '@karya/core';

import { BackendRequestInitAction } from '../../store/apis/APIs';

import { ErrorMessageWithRetry, ProgressBar } from '../templates/Status';

// HoCs
import { DataProps, withData } from '../hoc/WithData';

// Material Table
import MaterialTable from 'material-table';

// For CSV file download
import { CSVLink } from 'react-csv';

// CSS
import '../../css/worker/WorkerOverview.css';

// Data connector
const dataConnector = withData('task');

// Map state to props
const mapStateToProps = (state: RootState) => {
  const workers_data = state.all.worker.data;
  const status = state.all.worker.status;
  return { workers_data, status };
};

// Map dispatch to props
const mapDispatchToProps = (dispatch: any) => {
  return {
    // For getting workers' data
    getWorkersSummary: () => {
      const action: BackendRequestInitAction = {
        type: 'BR_INIT',
        store: 'worker',
        label: 'GET_ALL',
      };
      dispatch(action);
    },
  };
};

// Create the connector
const reduxConnector = connect(mapStateToProps, mapDispatchToProps);
const connector = compose(dataConnector, reduxConnector);

type WorkerOverviewProps = DataProps<typeof dataConnector> & ConnectedProps<typeof reduxConnector>;

// component state
type WorkerOverviewState = {
  tags_filter: Array<string>;
  box_id_filter?: string;
  sort_by?: string;
  graph_display: { assigned: boolean; completed: boolean; verified: boolean; earned: boolean };
  show_reg?: string;
};

// Task list component
class WorkerOverview extends React.Component<WorkerOverviewProps, WorkerOverviewState> {
  // Initial state
  state: WorkerOverviewState = {
    tags_filter: [],
    graph_display: { assigned: true, completed: true, verified: true, earned: false },
  };

  componentDidMount() {
    this.props.getWorkersSummary();
    M.AutoInit();
  }

  componentDidUpdate() {
    M.AutoInit();
  }

  // Handle tags change
  handleTagsChange: React.ChangeEventHandler<HTMLSelectElement> = (e) => {
    const tags_filter = Array.from(e.currentTarget.selectedOptions, (o) => o.value);
    this.setState({ tags_filter });
  };

  // Handle box id change
  handleBoxIdChange: React.ChangeEventHandler<HTMLSelectElement> = (e) => {
    const box_id_filter = e.currentTarget.value;
    this.setState({ box_id_filter });
  };

  // Handle change in sorting parameter
  handleSortByChange: React.ChangeEventHandler<HTMLInputElement> = (e) => {
    const sort_by = e.currentTarget.value;
    this.setState({ sort_by });
  };

  // Handle change in show_reg
  handleShowRegChange: React.ChangeEventHandler<HTMLInputElement> = (e) => {
    const show_reg = e.currentTarget.value;
    this.setState({ show_reg });
  };

  // Handle boolean input change
  handleBooleanChange: React.ChangeEventHandler<HTMLInputElement> = (e) => {
    const graph_display = { ...this.state.graph_display, [e.currentTarget.id]: e.currentTarget.checked };
    this.setState({ graph_display });
  };

  // Render component
  render() {
    type Extras = { assigned: number; completed: number; verified: number; earned: number };
    var workers = this.props.workers_data as (WorkerRecord & { extras: Extras })[];
    const tags_filter = this.state.tags_filter;
    const box_id_filter = this.state.box_id_filter;
    const sort_by = this.state.sort_by;
    const graph_display = this.state.graph_display;
    const show_reg = this.state.show_reg;

    // Filtering workers by tags
    workers = workers.filter((w) => tags_filter.every((val) => w.tags.tags.includes(val)));

    // Getting all the tasks' tags as a single flat array with no duplicates
    const tags_array = workers.map((w) => w.tags.tags);
    const arr: string[] = [];
    const tags_duplicates = arr.concat(...tags_array);
    const tags = Array.from(new Set([...tags_duplicates]));

    // Getting all the box ids as an array with no duplicates
    const boxIds_duplicates = workers.map((w) => w.box_id);
    const boxIds = Array.from(new Set([...boxIds_duplicates]));

    // Filtering workers by box id
    if (box_id_filter !== undefined && box_id_filter !== 'all') {
      workers = workers.filter((w) => w.box_id === box_id_filter);
    }

    // Filtering registered or unregistered workers
    if (show_reg === 'yes') {
      workers = workers.filter((w) => w.reg_mechanism !== null);
    } else if (show_reg === 'no') {
      workers = workers.filter((w) => w.reg_mechanism === null);
    }

    // Data to be fed into graph
    var data = workers.map((w) => {
      const startDate = new Date(w.registered_at);
      const endDate = new Date(w.sent_to_server_at);
      // Discard the time and time-zone information.
      const utc1 = Date.UTC(startDate.getFullYear(), startDate.getMonth(), startDate.getDate());
      const utc2 = Date.UTC(endDate.getFullYear(), endDate.getMonth(), endDate.getDate());
      const difference = Math.floor((utc2 - utc1) / 1000 * 60 * 60 * 24);
      var status;
      if (difference > 7) {
        status = "INACTIVE"
      } else {
        status = "ACTIVE"
      }

      return {
        id: w.id,
        access_code: w.access_code,
        startDate: w.registered_at,
        lastUpdated: w.sent_to_server_at,
        gender: w.gender,
        yearOfBirth: w.year_of_birth,
        phoneNumber: w.phone_number,
        status: status,
        ...w.extras,
      };
    });

    // Sorting the data
    if (sort_by !== undefined) {
      sort_by === 'completed'
        ? (data = data.sort((prev, next) => prev.completed - next.completed))
        : (data = data.sort((prev, next) => prev.verified - next.verified));
    }

    // Create error message element if necessary
    const getErrorElement =
      this.props.status === 'FAILURE' ? (
        <ErrorMessageWithRetry message={['Unable to fetch the data']} onRetry={this.props.getWorkersSummary} />
      ) : null;

    return (
      <div className='row main-row'>
        <div className='col s12'>
          {this.props.status === 'IN_FLIGHT' ? (
            <ProgressBar />
          ) : this.props.status === 'FAILURE' ? (
            <div>{getErrorElement}</div>
          ) : (
            <>
              <h1 className='page-title' id='workers-title'>
                Workers
              </h1>
              <div className='row' id='filter_row'>
                <div className='col s10 m8 l5'>
                  <select multiple={true} id='tags_filter' value={tags_filter} onChange={this.handleTagsChange}>
                    <option value='' disabled={true} selected={true}>
                      Filter workers by tags
                    </option>
                    {tags.map((t) => (
                      <option value={t} key={t}>
                        {t}
                      </option>
                    ))}
                  </select>
                </div>
                <div className='col s10 m8 l4'>
                  <select id='box_id_filter' value={box_id_filter} onChange={this.handleBoxIdChange}>
                    <option value='' disabled={true} selected={true}>
                      Filter workers by box ID
                    </option>
                    <option value='all'>All boxes</option>
                    {boxIds.map((i) => (
                      <option value={i} key={i}>
                        {i}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
              <div className='row' id='sort_row'>
                <p>Sort by: </p>
                <label key='completed'>
                  <input
                    type='radio'
                    className='with-gap'
                    name='sort_by'
                    value='completed'
                    onChange={this.handleSortByChange}
                  />
                  <span>Completed</span>
                </label>
                <label key='verified'>
                  <input
                    type='radio'
                    className='with-gap'
                    name='sort_by'
                    value='verified'
                    onChange={this.handleSortByChange}
                  />
                  <span>Verified</span>
                </label>
              </div>
              <div className='row' id='display_row'>
                <p>Display: </p>
                <label htmlFor='assigned'>
                  <input
                    type='checkbox'
                    className='filled-in'
                    id='assigned'
                    checked={graph_display.assigned}
                    onChange={this.handleBooleanChange}
                  />
                  <span>Assigned</span>
                </label>
                <label htmlFor='completed'>
                  <input
                    type='checkbox'
                    className='filled-in'
                    id='completed'
                    checked={graph_display.completed}
                    onChange={this.handleBooleanChange}
                  />
                  <span>Completed</span>
                </label>
                <label htmlFor='verified'>
                  <input
                    type='checkbox'
                    className='filled-in'
                    id='verified'
                    checked={graph_display.verified}
                    onChange={this.handleBooleanChange}
                  />
                  <span>Verified</span>
                </label>
                <label htmlFor='earned'>
                  <input
                    type='checkbox'
                    className='filled-in'
                    id='earned'
                    checked={graph_display.earned}
                    onChange={this.handleBooleanChange}
                  />
                  <span>Earned</span>
                </label>
              </div>
              <div className='row' id='table-row'>
                <div className='col s12' style={{ maxWidth: '100%' }}>
                  <MaterialTable
                    columns={[
                      { title: 'Access Code', field: 'access_code', type: 'string' },
                      { title: 'Phone Number', field: 'phoneNumber', type: 'string' },
                      { title: 'Gender', field: 'gender', type: 'string' },
                      { title: 'Year of Birth', field: 'yearOfBirth', type: 'string' },
                      { title: 'Registration Date', field: 'startDate', type: 'date' },
                      { title: 'Last Task Submission', field: 'lastUpdated', type: 'date' },
                      { title: 'Status', field: 'status', type: 'string' },
                      { title: 'Assigned', field: 'assigned', type: 'numeric' },
                      { title: 'Completed', field: 'completed', type: 'numeric' },
                      { title: 'Verified', field: 'verified', type: 'numeric' },
                      { title: 'Earned (In rupees)', field: 'earned', type: 'numeric' },
                    ]}
                    data={ data }
                    title='Workers'
                  />
                </div>
              </div>

              <div className='row' id='reg_row'>
                <p>Show </p>
                <label key='registered'>
                  <input
                    type='radio'
                    className='with-gap'
                    name='show_reg'
                    value='yes'
                    onChange={this.handleShowRegChange}
                  />
                  <span>Registered</span>
                </label>
                <label key='unregistered'>
                  <input
                    type='radio'
                    className='with-gap'
                    name='show_reg'
                    value='no'
                    onChange={this.handleShowRegChange}
                  />
                  <span>Unregistered</span>
                </label>
                <label key='all'>
                  <input
                    type='radio'
                    className='with-gap'
                    name='show_reg'
                    value='all'
                    onChange={this.handleShowRegChange}
                  />
                  <span>All</span>
                </label>
              </div>

              <CSVLink data={data} filename={'worker-data.csv'} className='btn' id='download-data-btn'>
                <i className='material-icons left'>download</i>Download data
              </CSVLink>
            </>
          )}
        </div>
      </div>
    );
  }
}

export default connector(WorkerOverview);
