import React from 'react';

import { createStyles, Theme, makeStyles } from '@material-ui/core/styles';

import Typography from '@material-ui/core/Typography';

import Candidate from './Candidate';

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        root: {
            width: '100%',
            maxWidth: 360,
            backgroundColor: theme.palette.background.paper,
        },
        nested: {
            paddingLeft: theme.spacing(4),
        },
        table: {
        },
    }),
);

export default function Candidates(props: any) {
    const { candidates } = props;
    const classes = useStyles();

    return (<>
        <Typography variant="h4" gutterBottom>Candidates</Typography>
        {candidates.map((candidate: any, i: number) => (
            <>
                <Typography variant="h6" gutterBottom>Candidate {i + 1}</Typography>
                <Candidate candidate={candidate} />
            </>
        ))}
    </>);
}